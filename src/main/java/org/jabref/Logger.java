package org.jabref;

/**
 * Separate class for logging.
 *
 * We use jcabi-log (see ADR-0001)
 *
 * We need to add a wrapper around it as developers won't write Logger.debug(this, "msg: %[exception]s", e) for every logged exception
 */
public class Logger {

    /** begin: direct pass-through **/

    public static void trace(final Object source, final String msg) {
        com.jcabi.log.Logger.trace(source, msg);
    }

    public static void trace(
            final Object source,
            final String msg, final Object... args
    ) {
        com.jcabi.log.Logger.trace(source, msg, args);
    }

    public static void debug(final Object source, final String msg) {
        com.jcabi.log.Logger.debug(source, msg);
    }

    public static void debug(
            final Object source,
            final String msg, final Object... args
    ) {
        com.jcabi.log.Logger.debug(source, msg, args);
    }

    public static void info(final Object source, final String msg) {
        com.jcabi.log.Logger.info(source, msg);
    }

    public static void info(
            final Object source,
            final String msg, final Object... args
    ) {
        com.jcabi.log.Logger.info(source, msg, args);
    }

    public static void warn(final Object source, final String msg) {
        com.jcabi.log.Logger.warn(source, msg);
    }

    public static void warn(
            final Object source,
            final String msg, final Object... args
    ) {
        com.jcabi.log.Logger.warn(source, msg, args);
    }

    public static void error(final Object source, final String msg) {
        com.jcabi.log.Logger.error(source, msg);
    }

    public static void error(final Object source,
                             final String msg, final Object... args) {
        com.jcabi.log.Logger.error(source, msg, args);
    }

    /** end of direct pass through **/


    public static void trace(final Object source, final String msg, Throwable e) {
        com.jcabi.log.Logger.trace(source, msg);
    }

    public static void trace(
            final Object source,
            final String msg, final Throwable e, final Object... args
    ) {
        if (com.jcabi.log.Logger.isTraceEnabled(source)) {
            com.jcabi.log.Logger.trace(source, msg + ": %[exception]s", e, args);
        }
    }

    public static void debug(final Object source, final Throwable e, final String msg) {
        com.jcabi.log.Logger.debug(source, msg);
    }

    public static void debug(
            final Object source,
            final String msg, final Throwable e, final Object... args
    ) {
        if (com.jcabi.log.Logger.isDebugEnabled(source)) {
            com.jcabi.log.Logger.debug(source, msg + ": %[exception]s", e, args);
        }
    }

    public static void info(final Object source, final Throwable e, final String msg) {
        com.jcabi.log.Logger.info(source, msg);
    }

    public static void info(final Object source, final Throwable e) {
        com.jcabi.log.Logger.info(source, "%[exception]s", e);
    }

    public static void info(
            final Object source,
            final String msg, final Throwable e, final Object... args
    ) {
        if (com.jcabi.log.Logger.isInfoEnabled(source)) {
            com.jcabi.log.Logger.info(source, msg + ": %[exception]s", e, args);
        }
    }

    public static void warn(final Object source, final Throwable e, final String msg) {
        com.jcabi.log.Logger.warn(source, msg);
    }

    public static void warn(final Object source, final Throwable e) {
        com.jcabi.log.Logger.warn(source, "%[exception]s", e);
    }

    public static void warn(
            final Object source,
            final String msg, final Throwable e, final Object... args
    ) {
        if (com.jcabi.log.Logger.isWarnEnabled(source)) {
            com.jcabi.log.Logger.warn(source, msg + ": %[exception]s", e, args);
        }
    }

    public static void error(final Object source, final Throwable e, final String msg) {
        com.jcabi.log.Logger.error(source, msg);
    }

    public static void error(final Object source,
                             final String msg, final Throwable e, final Object... args) {
        // error is always logged. Therefore, no isErrorEnabled(Object) exists
        com.jcabi.log.Logger.error(source, msg + ": %[exception]s", e, args);
    }
}
