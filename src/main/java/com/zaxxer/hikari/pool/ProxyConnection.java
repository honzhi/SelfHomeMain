package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.SQLExceptionOverride;
import com.zaxxer.hikari.util.ClockSource;
import com.zaxxer.hikari.util.FastList;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProxyConnection implements Connection {
    static final int DIRTY_BIT_READONLY = 1;
    static final int DIRTY_BIT_AUTOCOMMIT = 2;
    static final int DIRTY_BIT_ISOLATION = 4;
    static final int DIRTY_BIT_CATALOG = 8;
    static final int DIRTY_BIT_NETTIMEOUT = 16;
    static final int DIRTY_BIT_SCHEMA = 32;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConnection.class);
    private static final Set<String> ERROR_STATES = new HashSet<>();
    private static final Set<Integer> ERROR_CODES = new HashSet<>();
    protected Connection delegate;
    private final PoolEntry poolEntry;
    private final ProxyLeakTask leakTask;
    private final FastList<Statement> openStatements;
    private int dirtyBits;
    private long lastAccess;
    private boolean isCommitStateDirty;
    private boolean isReadOnly;
    private boolean isAutoCommit;
    private int networkTimeout;
    private int transactionIsolation;
    private String dbcatalog;
    private String dbschema;

    protected ProxyConnection(
        PoolEntry poolEntry,
        Connection connection,
        FastList<Statement> openStatements,
        ProxyLeakTask leakTask,
        long now,
        boolean isReadOnly,
        boolean isAutoCommit
    ) {
        this.poolEntry = poolEntry;
        this.delegate = connection;
        this.openStatements = openStatements;
        this.leakTask = leakTask;
        this.lastAccess = now;
        this.isReadOnly = isReadOnly;
        this.isAutoCommit = isAutoCommit;
    }

    @Override
    public final String toString() {
        return this.getClass().getSimpleName() + '@' + System.identityHashCode(this) + " wrapping " + this.delegate;
    }

    final boolean getAutoCommitState() {
        return this.isAutoCommit;
    }

    final String getCatalogState() {
        return this.dbcatalog;
    }

    final String getSchemaState() {
        return this.dbschema;
    }

    final int getTransactionIsolationState() {
        return this.transactionIsolation;
    }

    final boolean getReadOnlyState() {
        return this.isReadOnly;
    }

    final int getNetworkTimeoutState() {
        return this.networkTimeout;
    }

    final PoolEntry getPoolEntry() {
        return this.poolEntry;
    }

    final SQLException checkException(SQLException sqle) {
        boolean evict = false;
        SQLException nse = sqle;
        SQLExceptionOverride exceptionOverride = this.poolEntry.getPoolBase().exceptionOverride;

        for (int depth = 0; this.delegate != ProxyConnection.ClosedConnection.CLOSED_CONNECTION && nse != null && depth < 10; depth++) {
            String sqlState = nse.getSQLState();
            if (sqlState != null && sqlState.startsWith("08")
                || nse instanceof SQLTimeoutException
                || ERROR_STATES.contains(sqlState)
                || ERROR_CODES.contains(nse.getErrorCode())) {
                if (exceptionOverride == null || exceptionOverride.adjudicate(nse) != SQLExceptionOverride.Override.DO_NOT_EVICT) {
                    evict = true;
                }
                break;
            }

            nse = nse.getNextException();
        }

        if (evict) {
            SQLException exception = nse != null ? nse : sqle;
            LOGGER.warn(
                "{} - Connection {} marked as broken because of SQLSTATE({}), ErrorCode({})",
                this.poolEntry.getPoolName(),
                this.delegate,
                exception.getSQLState(),
                exception.getErrorCode(),
                exception
            );
            this.leakTask.cancel();
            this.poolEntry.evict("(connection is broken)");
            this.delegate = ProxyConnection.ClosedConnection.CLOSED_CONNECTION;
        }

        return sqle;
    }

    final synchronized void untrackStatement(Statement statement) {
        this.openStatements.remove(statement);
    }

    final void markCommitStateDirty() {
        if (this.isAutoCommit) {
            this.lastAccess = ClockSource.currentTime();
        } else {
            this.isCommitStateDirty = true;
        }
    }

    void cancelLeakTask() {
        this.leakTask.cancel();
    }

    private synchronized <T extends Statement> T trackStatement(T statement) {
        this.openStatements.add(statement);
        return statement;
    }

    private synchronized void closeStatements() {
        int size = this.openStatements.size();
        if (size > 0) {
            for (int i = 0; i < size && this.delegate != ProxyConnection.ClosedConnection.CLOSED_CONNECTION; i++) {
                try {
                    Statement ignored = this.openStatements.get(i);
                    if (ignored != null) {
                        ignored.close();
                    }
                } catch (SQLException e) {
                    LOGGER.warn(
                        "{} - Connection {} marked as broken because of an exception closing open statements during Connection.close()",
                        this.poolEntry.getPoolName(),
                        this.delegate
                    );
                    this.leakTask.cancel();
                    this.poolEntry.evict("(exception closing Statements during Connection.close())");
                    this.delegate = ProxyConnection.ClosedConnection.CLOSED_CONNECTION;
                }
            }

            this.openStatements.clear();
        }
    }

    @Override
    public final void close() throws SQLException {
        this.closeStatements();
        if (this.delegate != ProxyConnection.ClosedConnection.CLOSED_CONNECTION) {
            this.leakTask.cancel();

            try {
                if (this.isCommitStateDirty && !this.isAutoCommit) {
                    this.delegate.rollback();
                    this.lastAccess = ClockSource.currentTime();
                    LOGGER.debug("{} - Executed rollback on connection {} due to dirty commit state on close().", this.poolEntry.getPoolName(), this.delegate);
                }

                if (this.dirtyBits != 0) {
                    this.poolEntry.resetConnectionState(this, this.dirtyBits);
                    this.lastAccess = ClockSource.currentTime();
                }

                this.delegate.clearWarnings();
            } catch (SQLException e) {
                if (!this.poolEntry.isMarkedEvicted()) {
                    throw this.checkException(e);
                }
            } finally {
                this.delegate = ProxyConnection.ClosedConnection.CLOSED_CONNECTION;
                this.poolEntry.recycle(this.lastAccess);
            }
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.delegate == ProxyConnection.ClosedConnection.CLOSED_CONNECTION;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return ProxyFactory.getProxyStatement(this, this.trackStatement(this.delegate.createStatement()));
    }

    @Override
    public Statement createStatement(int resultSetType, int concurrency) throws SQLException {
        return ProxyFactory.getProxyStatement(this, this.trackStatement(this.delegate.createStatement(resultSetType, concurrency)));
    }

    @Override
    public Statement createStatement(int resultSetType, int concurrency, int holdability) throws SQLException {
        return ProxyFactory.getProxyStatement(this, this.trackStatement(this.delegate.createStatement(resultSetType, concurrency, holdability)));
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return ProxyFactory.getProxyCallableStatement(this, this.trackStatement(this.delegate.prepareCall(sql)));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int concurrency) throws SQLException {
        return ProxyFactory.getProxyCallableStatement(this, this.trackStatement(this.delegate.prepareCall(sql, resultSetType, concurrency)));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int concurrency, int holdability) throws SQLException {
        return ProxyFactory.getProxyCallableStatement(this, this.trackStatement(this.delegate.prepareCall(sql, resultSetType, concurrency, holdability)));
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return ProxyFactory.getProxyPreparedStatement(this, this.trackStatement(this.delegate.prepareStatement(sql)));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return ProxyFactory.getProxyPreparedStatement(this, this.trackStatement(this.delegate.prepareStatement(sql, autoGeneratedKeys)));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency) throws SQLException {
        return ProxyFactory.getProxyPreparedStatement(this, this.trackStatement(this.delegate.prepareStatement(sql, resultSetType, concurrency)));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency, int holdability) throws SQLException {
        return ProxyFactory.getProxyPreparedStatement(this, this.trackStatement(this.delegate.prepareStatement(sql, resultSetType, concurrency, holdability)));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return ProxyFactory.getProxyPreparedStatement(this, this.trackStatement(this.delegate.prepareStatement(sql, columnIndexes)));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return ProxyFactory.getProxyPreparedStatement(this, this.trackStatement(this.delegate.prepareStatement(sql, columnNames)));
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        this.markCommitStateDirty();
        return ProxyFactory.getProxyDatabaseMetaData(this, this.delegate.getMetaData());
    }

    @Override
    public void commit() throws SQLException {
        this.delegate.commit();
        this.isCommitStateDirty = false;
        this.lastAccess = ClockSource.currentTime();
    }

    @Override
    public void rollback() throws SQLException {
        this.delegate.rollback();
        this.isCommitStateDirty = false;
        this.lastAccess = ClockSource.currentTime();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        this.delegate.rollback(savepoint);
        this.isCommitStateDirty = false;
        this.lastAccess = ClockSource.currentTime();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.delegate.setAutoCommit(autoCommit);
        this.isAutoCommit = autoCommit;
        this.dirtyBits |= 2;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.delegate.setReadOnly(readOnly);
        this.isReadOnly = readOnly;
        this.isCommitStateDirty = false;
        this.dirtyBits |= 1;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        this.delegate.setTransactionIsolation(level);
        this.transactionIsolation = level;
        this.dirtyBits |= 4;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        this.delegate.setCatalog(catalog);
        this.dbcatalog = catalog;
        this.dirtyBits |= 8;
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        this.delegate.setNetworkTimeout(executor, milliseconds);
        this.networkTimeout = milliseconds;
        this.dirtyBits |= 16;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        this.delegate.setSchema(schema);
        this.dbschema = schema;
        this.dirtyBits |= 32;
    }

    @Override
    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this.delegate) || this.delegate != null && this.delegate.isWrapperFor(iface);
    }

    @Override
    public final <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this.delegate)) {
            return (T)this.delegate;
        } else if (this.delegate != null) {
            return this.delegate.unwrap(iface);
        } else {
            throw new SQLException("Wrapped connection is not an instance of " + iface);
        }
    }

    static {
        ERROR_STATES.add("0A000");
        ERROR_STATES.add("57P01");
        ERROR_STATES.add("57P02");
        ERROR_STATES.add("57P03");
        ERROR_STATES.add("01002");
        ERROR_STATES.add("JZ0C0");
        ERROR_STATES.add("JZ0C1");
        ERROR_CODES.add(500150);
        ERROR_CODES.add(2399);
    }

    private static final class ClosedConnection {
        static final Connection CLOSED_CONNECTION = getClosedConnection();

        private static Connection getClosedConnection() {
            InvocationHandler handler = (proxy, method, args) -> {
                String methodName = method.getName();
                if ("isClosed".equals(methodName)) {
                    return Boolean.TRUE;
                } else if ("isValid".equals(methodName)) {
                    return Boolean.FALSE;
                } else if ("abort".equals(methodName)) {
                    return void.class;
                } else if ("close".equals(methodName)) {
                    return void.class;
                } else if ("toString".equals(methodName)) {
                    return ProxyConnection.ClosedConnection.class.getCanonicalName();
                } else {
                    throw new SQLException("Connection is closed");
                }
            };
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[]{Connection.class}, handler);
        }
    }
}
