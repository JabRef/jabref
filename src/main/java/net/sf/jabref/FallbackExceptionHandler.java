package net.sf.jabref;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Catch and log any unhandled exceptions.
 */
public class FallbackExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Marker UncaughtException_MARKER = MarkerManager.getMarker("UncaughtException");

    public static void installExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new FallbackExceptionHandler());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        Logger logger = LogManager.getLogger(FallbackExceptionHandler.class);
        logger.error(UncaughtException_MARKER, "Uncaught exception Occurred in " + thread, exception);
    }
}
