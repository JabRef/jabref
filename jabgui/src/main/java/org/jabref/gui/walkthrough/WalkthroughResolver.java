package org.jabref.gui.walkthrough;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.util.RecursiveChildrenListener;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughResolver.class);
    private static final Duration RESOLVE_TIMEOUT = Duration.millis(2_500);
    private static final int DEBOUNCE_DELAY_MS = 200;

    private final WindowResolver windowResolver;
    private final @Nullable NodeResolver nodeResolver;
    private final Consumer<WalkthroughResult> onCompletion;
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    private @Nullable Runnable windowListenerCleanup;
    private @Nullable ChangeListener<Scene> sceneListener;
    private @Nullable RecursiveChildrenListener recursiveChildrenListener;
    private @Nullable DelayedExecution delayedExecution;

    /// Creates a [WalkthroughResolver] that attempts to identify the window and the
    /// node from the
    /// [org.jabref.gui.walkthrough.declarative.step.VisibleComponent#windowResolver()]
    /// and
    /// [org.jabref.gui.walkthrough.declarative.step.VisibleComponent#nodeResolver()]
    /// and supplies [WalkthroughResult] to a specified [#onCompletion] consumer.
    ///
    /// @implNote 1. The entire resolve process finishes within 2.5 seconds or when the
    /// nodes are all shown, whichever comes first.
    /// 2. The resolver first tries to resolve the window, then resolves the node from
    /// the scene on the window using an event-based approach. Specifically:
    ///    1. Re-resolution of window is triggered upon creation or deletion of a
    /// window.
    ///    2. If a scene is not immediately present in the window,
    /// [Window#sceneProperty()] is listened to until a scene is present.
    ///    3. Re-resolution of node is triggered upon any children list change in the
    /// scenegraph, or [com.sun.javafx.scene.TreeShowingProperty] change of any node in
    /// the scene graph.
    /// 3. [#onCompletion] is guaranteed to be called regardless of whether resolution
    /// is successful on a JavaFX thread upon [#startResolution()].
    ///
    /// You may NOT use this class to resolve more than once. In such case,
    /// [#onCompletion] will never be called.
    public WalkthroughResolver(WindowResolver windowResolver,
                               @Nullable NodeResolver nodeResolver,
                               Consumer<WalkthroughResult> onCompletion) {
        this.windowResolver = windowResolver;
        this.nodeResolver = nodeResolver;
        this.onCompletion = onCompletion;
    }

    public void startResolution() {
        delayedExecution = new DelayedExecution(RESOLVE_TIMEOUT, () -> {
            LOGGER.error("Walkthrough resolution timed out.");
            finish(new WalkthroughResult(null, null));
        });
        delayedExecution.start();

        Optional<Window> window = windowResolver.resolve();
        if (window.isPresent()) {
            handleWindowResolved(window.get());
        } else {
            listenForWindow(windowResolver);
        }
    }

    private void listenForWindow(WindowResolver resolver) {
        AtomicBoolean windowFound = new AtomicBoolean(false);
        this.windowListenerCleanup = WalkthroughUtils.onWindowChangedUntil(() -> {
            Optional<Window> window = resolver.resolve();
            if (window.isPresent()) {
                windowFound.set(true);
                handleWindowResolved(window.get());
            }
        }, windowFound::get);
    }

    private void handleWindowResolved(Window window) {
        if (nodeResolver == null) {
            finish(new WalkthroughResult(window, null));
            return;
        }

        if (window.getScene() != null) {
            handleSceneResolved(window, window.getScene(), nodeResolver);
        } else {
            listenForScene(window, nodeResolver);
        }
    }

    private void listenForScene(Window window, NodeResolver resolver) {
        sceneListener = (_, _, newScene) -> {
            if (newScene != null) {
                window.sceneProperty().removeListener(sceneListener);
                sceneListener = null;
                handleSceneResolved(window, newScene, resolver);
            }
        };
        window.sceneProperty().addListener(sceneListener);
    }

    private void handleSceneResolved(Window window, Scene scene, NodeResolver resolver) {
        Optional<Node> node = resolver.resolve(scene);
        if (node.isPresent()) {
            finish(new WalkthroughResult(window, node.get()));
        } else {
            listenForNodeInScene(window, scene, resolver);
        }
    }

    private void listenForNodeInScene(Window window, Scene scene, NodeResolver resolver) {
        InvalidationListener debouncedNodeFinder = WalkthroughUtils.debounced(_ -> {
            Optional<Node> node = resolver.resolve(scene);
            if (node.isPresent()) {
                detachChildrenListener();
                finish(new WalkthroughResult(window, node.get()));
            }
        }, DEBOUNCE_DELAY_MS);

        recursiveChildrenListener = new RecursiveChildrenListener(debouncedNodeFinder);
        recursiveChildrenListener.attachToScene(scene);
    }

    private void finish(WalkthroughResult result) {
        if (isFinished.compareAndSet(false, true)) {
            cancel();
            UiTaskExecutor.runInJavaFXThread(() -> onCompletion.accept(result));
        }
    }

    public void cancel() {
        if (delayedExecution != null) {
            delayedExecution.cancel();
            delayedExecution = null;
        }
        if (windowListenerCleanup != null) {
            windowListenerCleanup.run();
            windowListenerCleanup = null;
        }
        detachChildrenListener();
    }

    private void detachChildrenListener() {
        if (recursiveChildrenListener != null) {
            recursiveChildrenListener.detach();
            recursiveChildrenListener = null;
        }
    }

    public record WalkthroughResult(Window windowValue, Node nodeValue) {
        public Optional<Window> window() {
            return Optional.ofNullable(windowValue);
        }

        public Optional<Node> node() {
            return Optional.ofNullable(nodeValue);
        }

        public boolean wasSuccessful() {
            return window().isPresent();
        }
    }
}
