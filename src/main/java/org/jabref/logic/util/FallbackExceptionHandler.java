package org.jabref.logic.util;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catch and log any unhandled exceptions.
 */
public class FallbackExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackExceptionHandler.class);

    private final BiConsumer<Throwable, Thread> onException;

    public FallbackExceptionHandler(BiConsumer<Throwable, Thread> onException) {
        this.onException = onException;
    }

    public FallbackExceptionHandler() {
        this(null);
    }

    public static void installExceptionHandler(BiConsumer<Throwable, Thread> onException) {
        Thread.setDefaultUncaughtExceptionHandler(new FallbackExceptionHandler(onException));
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        LOGGER.error("Uncaught exception occurred in {}", thread, exception);
        if (this.onException != null) {
            this.onException.accept(exception, thread);
        }
    }
}
