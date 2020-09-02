package org.jabref.gui;

import org.jabref.gui.util.DefaultTaskExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catch and log any unhandled exceptions.
 */
public class FallbackExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackExceptionHandler.class);

    public static void installExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new FallbackExceptionHandler());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        LOGGER.error("Uncaught exception occurred in " + thread, exception);

        DefaultTaskExecutor.runInJavaFXThread(() ->
                JabRefGUI.getMainFrame()
                         .getDialogService()
                         .showErrorDialogAndWait("Uncaught exception occurred in " + thread, exception)
        );
    }
}
