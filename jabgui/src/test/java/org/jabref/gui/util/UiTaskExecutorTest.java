package org.jabref.gui.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.collections.FXCollections;

import org.jabref.gui.StateManager;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.util.SerialBackgroundTaskGroup;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void cancellingGroupKeepsResourceOpenUntilJavaFxWorkerExits() throws Exception {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getBackgroundTasks()).thenReturn(FXCollections.observableArrayList());
        Injector.setModelOrService(StateManager.class, stateManager);
        UiTaskExecutor executor = new UiTaskExecutor();
        SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(executor);
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<Void> release = new CompletableFuture<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        try (ExecutorService waiters = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                group.submit(task -> {
                    started.countDown();
                    release.join();
                    cancelled.set(task.isCancelled());
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                JavaFxExtension.invokeAndWait(group::cancel);
                Future<?> closing = waiters.submit(() -> group.closeAndWait(() -> closed.set(true)));
                assertFalse(closed.get());
                release.complete(null);
                closing.get(5, TimeUnit.SECONDS);
                assertTrue(cancelled.get());
                assertTrue(closed.get());
            } finally {
                release.complete(null);
                executor.shutdown();
                Injector.forgetAll();
            }
        }
    }
}
