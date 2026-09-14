package org.jabref.logic.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallbackExceptionHandlerTest {

    private final List<Throwable> reported = new ArrayList<>();
    private final FallbackExceptionHandler handler = new FallbackExceptionHandler((exception, _) -> reported.add(exception));

    @Test
    void sshdExecutorShutdownIsNotReported() {
        handler.uncaughtException(Thread.currentThread(), exceptionThrownBy("org.apache.sshd.common.util.ValidateUtils", "Executor has been shut down"));

        assertEquals(List.of(), reported);
    }

    @Test
    void sameMessageFromOtherCodeIsReported() {
        IllegalStateException exception = exceptionThrownBy("org.jabref.Foo", "Executor has been shut down");

        handler.uncaughtException(Thread.currentThread(), exception);

        assertEquals(List.of(exception), reported);
    }

    private static IllegalStateException exceptionThrownBy(String className, String message) {
        IllegalStateException exception = new IllegalStateException(message);
        exception.setStackTrace(new StackTraceElement[] {new StackTraceElement(className, "createFormattedException", null, 1)});
        return exception;
    }
}
