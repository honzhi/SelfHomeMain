package org.slf4j.event;

import org.slf4j.Marker;
import org.slf4j.helpers.SubstituteLogger;

public class SubstituteLoggingEvent implements LoggingEvent {
    Level level;
    Marker marker;
    String loggerName;
    SubstituteLogger logger;
    String threadName;
    String message;
    Object[] argArray;
    long timeStamp;
    Throwable throwable;

    @Override
    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    @Override
    public Marker getMarker() {
        return this.marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    @Override
    public String getLoggerName() {
        return this.loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public SubstituteLogger getLogger() {
        return this.logger;
    }

    public void setLogger(SubstituteLogger logger) {
        this.logger = logger;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Object[] getArgumentArray() {
        return this.argArray;
    }

    public void setArgumentArray(Object[] argArray) {
        this.argArray = argArray;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public String getThreadName() {
        return this.threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public Throwable getThrowable() {
        return this.throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
