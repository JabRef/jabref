package org.jabref.gui.walkthrough;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.util.RecursiveChildrenListener;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughResolver.class);
    private static final Duration RESOLVE_TIMEOUT = Duration.millis(20_000);
    private static final int DEBOUNCE_DELAY_MS = 200;

    private final WindowResolver windowResolver;
    private final @Nullable NodeResolver nodeResolver;
    private final Consumer<WalkthroughResult> onCompletion;
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    private @Nullable Runnable windowListenerCleanup;
    private @Nullable ChangeListener<Scene> sceneListener;
    private @Nullable RecursiveChildrenListener recursiveChildrenListener;
    private @Nullable Timeout timeout;

    public WalkthroughResolver(WindowResolver windowResolver,
                               @Nullable NodeResolver nodeResolver,
                               Consumer<WalkthroughResult> onCompletion) {
        this.windowResolver = windowResolver;
        this.nodeResolver = nodeResolver;
        this.onCompletion = onCompletion;
    }

    public void startResolution() {
        timeout = new Timeout(RESOLVE_TIMEOUT, () -> {
            LOGGER.error("Walkthrough resolution timed out.");
            finish(new WalkthroughResult(null, null));
        });
        timeout.start();

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
            Platform.runLater(() -> onCompletion.accept(result));
        }
    }

    public void cancel() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
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
