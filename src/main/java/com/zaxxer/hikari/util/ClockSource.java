package com.zaxxer.hikari.util;

import java.util.concurrent.TimeUnit;

public interface ClockSource {
    ClockSource CLOCK = ClockSource.Factory.create();
    TimeUnit[] TIMEUNITS_DESCENDING = new TimeUnit[]{
        TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS
    };
    String[] TIMEUNIT_DISPLAY_VALUES = new String[]{"ns", "µs", "ms", "s", "m", "h", "d"};

    static long currentTime() {
        return CLOCK.currentTime0();
    }

    long currentTime0();

    static long toMillis(long time) {
        return CLOCK.toMillis0(time);
    }

    long toMillis0(long var1);

    static long toNanos(long time) {
        return CLOCK.toNanos0(time);
    }

    long toNanos0(long var1);

    static long elapsedMillis(long startTime) {
        return CLOCK.elapsedMillis0(startTime);
    }

    long elapsedMillis0(long var1);

    static long elapsedMillis(long startTime, long endTime) {
        return CLOCK.elapsedMillis0(startTime, endTime);
    }

    long elapsedMillis0(long var1, long var3);

    static long elapsedNanos(long startTime) {
        return CLOCK.elapsedNanos0(startTime);
    }

    long elapsedNanos0(long var1);

    static long elapsedNanos(long startTime, long endTime) {
        return CLOCK.elapsedNanos0(startTime, endTime);
    }

    long elapsedNanos0(long var1, long var3);

    static long plusMillis(long time, long millis) {
        return CLOCK.plusMillis0(time, millis);
    }

    long plusMillis0(long var1, long var3);

    static TimeUnit getSourceTimeUnit() {
        return CLOCK.getSourceTimeUnit0();
    }

    TimeUnit getSourceTimeUnit0();

    static String elapsedDisplayString(long startTime, long endTime) {
        return CLOCK.elapsedDisplayString0(startTime, endTime);
    }

    default String elapsedDisplayString0(long startTime, long endTime) {
        long elapsedNanos = this.elapsedNanos0(startTime, endTime);
        StringBuilder sb = new StringBuilder(elapsedNanos < 0L ? "-" : "");
        elapsedNanos = Math.abs(elapsedNanos);

        for (TimeUnit unit : TIMEUNITS_DESCENDING) {
            long converted = unit.convert(elapsedNanos, TimeUnit.NANOSECONDS);
            if (converted > 0L) {
                sb.append(converted).append(TIMEUNIT_DISPLAY_VALUES[unit.ordinal()]);
                elapsedNanos -= TimeUnit.NANOSECONDS.convert(converted, unit);
            }
        }

        return sb.toString();
    }

    class Factory {
        private static ClockSource create() {
            String os = System.getProperty("os.name");
            return "Mac OS X".equals(os) ? new ClockSource.MillisecondClockSource() : new ClockSource.NanosecondClockSource();
        }
    }

    final class MillisecondClockSource implements ClockSource {
        @Override
        public long currentTime0() {
            return System.currentTimeMillis();
        }

        @Override
        public long elapsedMillis0(long startTime) {
            return System.currentTimeMillis() - startTime;
        }

        @Override
        public long elapsedMillis0(long startTime, long endTime) {
            return endTime - startTime;
        }

        @Override
        public long elapsedNanos0(long startTime) {
            return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis() - startTime);
        }

        @Override
        public long elapsedNanos0(long startTime, long endTime) {
            return TimeUnit.MILLISECONDS.toNanos(endTime - startTime);
        }

        @Override
        public long toMillis0(long time) {
            return time;
        }

        @Override
        public long toNanos0(long time) {
            return TimeUnit.MILLISECONDS.toNanos(time);
        }

        @Override
        public long plusMillis0(long time, long millis) {
            return time + millis;
        }

        @Override
        public TimeUnit getSourceTimeUnit0() {
            return TimeUnit.MILLISECONDS;
        }
    }

    class NanosecondClockSource implements ClockSource {
        @Override
        public long currentTime0() {
            return System.nanoTime();
        }

        @Override
        public long toMillis0(long time) {
            return TimeUnit.NANOSECONDS.toMillis(time);
        }

        @Override
        public long toNanos0(long time) {
            return time;
        }

        @Override
        public long elapsedMillis0(long startTime) {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        }

        @Override
        public long elapsedMillis0(long startTime, long endTime) {
            return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        }

        @Override
        public long elapsedNanos0(long startTime) {
            return System.nanoTime() - startTime;
        }

        @Override
        public long elapsedNanos0(long startTime, long endTime) {
            return endTime - startTime;
        }

        @Override
        public long plusMillis0(long time, long millis) {
            return time + TimeUnit.MILLISECONDS.toNanos(millis);
        }

        @Override
        public TimeUnit getSourceTimeUnit0() {
            return TimeUnit.NANOSECONDS;
        }
    }
}
