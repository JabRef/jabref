package org.jabref.gui.testutils;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/// Initializes the JavaFX toolkit and invokes a test's JavaFX setup method before each test.
@NullMarked
public class JavaFxExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(JavaFxExtension.class);
    private static boolean toolkitInitialized;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        initializeToolkit();

        Object testInstance = context.getRequiredTestInstance();
        Stage stage = callAndWait(Stage::new);
        context.getStore(NAMESPACE).put(context.getUniqueId(), stage);

        if (testInstance instanceof JavaFxTest javaFxTest) {
            runAndWait(() -> javaFxTest.start(stage));
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Stage stage = context.getStore(NAMESPACE).remove(context.getUniqueId(), Stage.class);
        Optional.ofNullable(stage).ifPresent(stageToClose -> runAndWait(stageToClose::close));
    }

    public static void runAndWait(Runnable action) {
        callAndWait(() -> {
            action.run();
            return null;
        });
    }

    public static void awaitEvents() {
        runAndWait(() -> {
        });
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

    private static <T> T callAndWait(Supplier<T> action) {
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
