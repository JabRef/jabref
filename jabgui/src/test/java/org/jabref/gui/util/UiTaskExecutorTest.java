package org.jabref.gui.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(JavaFxExtension.class)
@NullMarked
class UiTaskExecutorTest {

    @Test
    void runAndWaitInJavaFXThreadWithFailurePropagationPropagatesBackgroundThreadFailure() throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<ExecutionException> future = executor.submit(() -> assertThrows(
                    ExecutionException.class,
                    () -> UiTaskExecutor.runAndWaitInJavaFXThreadWithFailurePropagation(() -> {
                        throw new IllegalArgumentException("expected failure");
                    })));

            ExecutionException exception = future.get();

            assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
        }
    }
}
