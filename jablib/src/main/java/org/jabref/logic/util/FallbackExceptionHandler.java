package org.jabref.logic.util;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Catch and log any unhandled exceptions.
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
        if (isSshdExecutorShutdown(exception)) {
            // Known Apache SSHD issue on Windows, harmless: https://github.com/apache/mina-sshd/issues/409
            // A dead SSH socket (e.g., after resume from suspend) reports its read failure after SSHD closed its executor.
            LOGGER.debug("Ignoring SSHD executor shutdown in {}", thread, exception);
            return;
        }
        LOGGER.error("Uncaught exception occurred in {}", thread, exception);
        if (this.onException != null) {
            this.onException.accept(exception, thread);
        }
    }

    private static boolean isSshdExecutorShutdown(Throwable exception) {
        return exception instanceof IllegalStateException
                && "Executor has been shut down".equals(exception.getMessage())
                && exception.getStackTrace().length > 0
                && exception.getStackTrace()[0].getClassName().startsWith("org.apache.sshd.");
    }
}
