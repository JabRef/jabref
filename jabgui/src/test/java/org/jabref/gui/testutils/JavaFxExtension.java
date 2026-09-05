package org.jabref.gui.testutils;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/// Initializes the JavaFX toolkit and invokes a test's JavaFX setup method before each test.
@NullMarked
public class JavaFxExtension implements BeforeEachCallback, AfterEachCallback {

    private static final int EVENT_DRAIN_PASSES = 5;
    private static final ReentrantLock TEST_LOCK = new ReentrantLock();
    private static final ConcurrentLinkedQueue<Throwable> ASYNCHRONOUS_FAILURES = new ConcurrentLinkedQueue<>();
    private static boolean toolkitInitialized;
    private static Thread.@Nullable UncaughtExceptionHandler previousFxUncaughtExceptionHandler;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        TEST_LOCK.lock();
        try {
            initializeToolkit();
            ASYNCHRONOUS_FAILURES.clear();
            installFailureHandler();

            Object testInstance = context.getRequiredTestInstance();
            Stage stage = invokeAndWait(Stage::new);

            if (testInstance instanceof JavaFxTest javaFxTest) {
                runAndWait(() -> javaFxTest.start(stage));
            }
        } catch (InterruptedException exception) {
            TEST_LOCK.unlock();
            throw exception;
        } catch (RuntimeException | Error exception) {
            TEST_LOCK.unlock();
            throw exception;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            awaitEvents();
        } finally {
            try {
                runAndWait(() -> Window.getWindows().forEach(Window::hide));
                restoreFailureHandler();
            } finally {
                TEST_LOCK.unlock();
            }
        }
    }

    public static void runAndWait(Runnable action) {
        invokeAndWait(() -> {
            action.run();
            return true;
        });
    }

    public static void awaitEvents() {
        for (int pass = 0; pass < EVENT_DRAIN_PASSES; pass++) {
            runAndWait(() -> {
            });
        }
        throwAsynchronousFailures();
    }

    private static synchronized void initializeToolkit() throws InterruptedException {
        if (toolkitInitialized) {
            return;
        }

        CountDownLatch toolkitStarted = new CountDownLatch(1);
        try {
            Platform.startup(toolkitStarted::countDown);
            toolkitStarted.await();
        } catch (IllegalStateException exception) {
            // The toolkit was initialized by another JavaFX consumer in this test JVM.
        }
        Platform.setImplicitExit(false);
        toolkitInitialized = true;
    }

    private static void installFailureHandler() {
        runAndWait(() -> {
            Thread fxApplicationThread = Thread.currentThread();
            previousFxUncaughtExceptionHandler = fxApplicationThread.getUncaughtExceptionHandler();
            fxApplicationThread.setUncaughtExceptionHandler((thread, throwable) -> ASYNCHRONOUS_FAILURES.add(throwable));
        });
    }

    private static void restoreFailureHandler() {
        runAndWait(() -> {
            Thread fxApplicationThread = Thread.currentThread();
            Optional.ofNullable(previousFxUncaughtExceptionHandler)
                    .ifPresent(fxApplicationThread::setUncaughtExceptionHandler);
            previousFxUncaughtExceptionHandler = null;
        });
    }

    private static void throwAsynchronousFailures() {
        @Nullable Throwable failure = ASYNCHRONOUS_FAILURES.poll();
        if (failure == null) {
            return;
        }

        AssertionError assertionError = new AssertionError("JavaFX asynchronous action failed", failure);
        while ((failure = ASYNCHRONOUS_FAILURES.poll()) != null) {
            assertionError.addSuppressed(failure);
        }
        throw assertionError;
    }

    private static <T> T invokeAndWait(Supplier<T> action) {
        if (Platform.isFxApplicationThread()) {
            return action.get();
        }

        FutureTask<T> future = new FutureTask<>(action::get);
        Platform.runLater(future);
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("JavaFX test action was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new AssertionError("JavaFX test action failed", exception.getCause());
        }
    }
}
