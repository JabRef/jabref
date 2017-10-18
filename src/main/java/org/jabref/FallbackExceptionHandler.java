package org.jabref;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Catch and log any unhandled exceptions.
 */
public class FallbackExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Log LOGGER = LogFactory.getLog(FallbackExceptionHandler.class);

    public static void installExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new FallbackExceptionHandler());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        LOGGER.error("Uncaught exception occurred in " + thread, exception);
    }
}
