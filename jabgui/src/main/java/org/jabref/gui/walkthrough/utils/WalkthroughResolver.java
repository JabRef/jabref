package org.jabref.gui.walkthrough.utils;

import java.util.Optional;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.util.RecursiveChildrenListener;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughResolver.class);
    private static final Duration RESOLVE_TIMEOUT = Duration.millis(2_500);
    private static final Duration NODE_IDLE_TIMEOUT = Duration.millis(250);

    private final WindowResolver windowResolver;
    private final @Nullable NodeResolver nodeResolver;
    private final Consumer<WalkthroughResult> onCompletion;

    private @Nullable Runnable windowListenerCleanup;
    private @Nullable ChangeListener<Scene> sceneListener;
    private @Nullable RecursiveChildrenListener recursiveChildrenListener;
    private WalkthroughUtils.@Nullable DebouncedInvalidationListener debouncedNodeFinder;

    private @Nullable DelayedExecution timeout;
    private @Nullable DelayedExecution nodeIdleTimeout;

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
        timeout = new DelayedExecution(RESOLVE_TIMEOUT, () -> {
            LOGGER.error("Walkthrough resolution timed out.");
            finish(new WalkthroughResult(null, null));
        });
        timeout.start();

        windowResolver.resolve().ifPresentOrElse(
                this::handleWindowResolved,
                () -> listenForWindow(windowResolver)
        );
    }

    private void listenForWindow(WindowResolver resolver) {
        this.windowListenerCleanup = WalkthroughUtils.onWindowChangedUntil(() -> {
            Optional<Window> window = resolver.resolve();
            if (window.isPresent()) {
                Platform.runLater(() -> handleWindowResolved(window.get())); // Make sure window listener detaches first.
                return true;
            }
            return false;
        });
    }

    private void handleWindowResolved(Window window) {
        if (nodeResolver == null) {
            finish(new WalkthroughResult(window, null));
            return;
        }

        Scene scene = window.getScene();
        if (scene != null) {
            handleSceneResolved(window, scene, nodeResolver);
        } else {
            listenForScene(window, nodeResolver);
        }
    }

    private void listenForScene(Window window, NodeResolver resolver) {
        sceneListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
                if (newScene != null) {
                    window.sceneProperty().removeListener(this);
                    sceneListener = null;
                    handleSceneResolved(window, newScene, resolver);
                }
            }
        };
        window.sceneProperty().addListener(sceneListener);
    }

    private void handleSceneResolved(Window window, Scene scene, NodeResolver resolver) {
        resolver.resolve(scene).ifPresentOrElse(
                node -> finish(new WalkthroughResult(window, node)),
                () -> listenForNodeInScene(window, scene, resolver)
        );
    }

    private void listenForNodeInScene(Window window, Scene scene, NodeResolver resolver) {
        debouncedNodeFinder = WalkthroughUtils.debounced(new InvalidationListener() {
            private @Nullable Node node = null;
            private boolean handled = false;

            @Override
            public void invalidated(Observable observable) {
                if (handled) {
                    return;
                }

                Optional<Node> node = resolver.resolve(scene);
                if (node.isEmpty()) {
                    return;
                }
                if (this.node == node.get()) {
                    return;
                }
                this.node = node.get();
                if (nodeIdleTimeout != null) {
                    nodeIdleTimeout.cancel();
                }
                // If a new node is resolved after listening, wait for a short period of time to see if it stays the same.
                LOGGER.info("Node resolved, waiting for it to stay the same: {}", node.get());
                nodeIdleTimeout = new DelayedExecution(NODE_IDLE_TIMEOUT, () -> {
                    LOGGER.info("Node idle timeout. The node has stayed the same: {}", node.get());
                    handled = true;
                    detachChildrenListener();
                    finish(new WalkthroughResult(window, this.node));
                });
                nodeIdleTimeout.start();
            }
        });

        recursiveChildrenListener = new RecursiveChildrenListener(debouncedNodeFinder);
        recursiveChildrenListener.attachToScene(scene);
    }

    private void finish(WalkthroughResult result) {
        cancel();
        onCompletion.accept(result);
    }

    public void cancel() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
        if (nodeIdleTimeout != null) {
            nodeIdleTimeout.cancel();
            nodeIdleTimeout = null;
        }
        sceneListener = null;
        if (windowListenerCleanup != null) {
            windowListenerCleanup.run();
            windowListenerCleanup = null;
        }
        if (debouncedNodeFinder != null) {
            debouncedNodeFinder.cancel();
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
