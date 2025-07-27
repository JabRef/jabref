package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.RecursiveChildrenListener;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Multi-window overlay for displaying walkthrough steps
public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);
    private static final long RESOLUTION_TIMEOUT_MS = 2500;

    private final Map<Window, WindowOverlay> overlays = new HashMap<>();
    private final Stage stage;
    private final WalkthroughHighlighter walkthroughHighlighter;
    private final Walkthrough walkthrough;

    /// Listener for waiting till window is resolved
    private @Nullable ListChangeListener<Window> windowListListener;
    /// Listener for waiting till scene to be set to attempt node resolution
    private @Nullable ChangeListener<Scene> sceneListener;
    /// Recursive listener for scene tree changes to attempt node resolution
    private @Nullable RecursiveChildrenListener recursiveChildrenListener;

    /// Listeners for reverting when window closed
    private @Nullable ChangeListener<Boolean> windowShowingListener;
    /// Listener for reverting when node visibility changes
    private @Nullable ChangeListener<Boolean> nodeVisibleListener;

    private @Nullable Window resolvedWindow;
    private @Nullable Node resolvedNode;
    private @Nullable PauseTransition timeoutTransition;

    public WalkthroughOverlay(Stage stage, Walkthrough walkthrough) {
        this.stage = stage;
        this.walkthrough = walkthrough;
        this.walkthroughHighlighter = new WalkthroughHighlighter();
        this.walkthroughHighlighter.setOnBackgroundClick(this::showQuitConfirmationAndQuit);
    }

    public void show(@NonNull WalkthroughStep step) {
        LOGGER.debug("Showing step: {}", step.title());
        cleanUp();
        overlays.values().forEach(WindowOverlay::hide);

        step.activeWindowResolver()
            .ifPresentOrElse(
                    resolver -> resolveWindow(step, resolver),
                    () -> handleWindowResolved(step, stage));
    }

    public void detachAll() {
        cleanUp();
        walkthroughHighlighter.detachAll();
        overlays.values().forEach(WindowOverlay::detach);
        overlays.clear();
    }

    /// Shows a confirmation dialog and quits the walkthrough if confirmed.
    public void showQuitConfirmationAndQuit() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        boolean shouldQuit = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Quit walkthrough"),
                Localization.lang("Are you sure you want to quit the walkthrough?"),
                Localization.lang("Quit"),
                Localization.lang("Continue walkthrough")
        );

        if (shouldQuit) {
            walkthrough.quit();
        }
    }

    private void cleanUp() {
        LOGGER.debug("Cleaning up all listeners");

        cancelTimeout();

        if (windowListListener != null) {
            Window.getWindows().removeListener(windowListListener);
            windowListListener = null;
        }

        if (resolvedWindow != null) {
            if (windowShowingListener != null) {
                resolvedWindow.showingProperty().removeListener(windowShowingListener);
                windowShowingListener = null;
            }
            if (sceneListener != null) {
                resolvedWindow.sceneProperty().removeListener(sceneListener);
                sceneListener = null;
            }
        }

        if (recursiveChildrenListener != null) {
            recursiveChildrenListener.detach();
            recursiveChildrenListener = null;
        }

        if (resolvedNode != null) {
            if (nodeVisibleListener != null) {
                resolvedNode.visibleProperty().removeListener(nodeVisibleListener);
                nodeVisibleListener = null;
            }
        }

        resolvedWindow = null;
        resolvedNode = null;
    }

    private void resolveWindow(WalkthroughStep step, WindowResolver resolver) {
        startTimeout(() -> {
            if (resolvedWindow == null) {
                LOGGER.warn("Timeout reached while waiting for window resolution for step '{}'. Reverting.", step.title());
                cleanUp();
                revertToPreviousStep();
            }
        });

        resolver.resolve().ifPresentOrElse(
                window -> {
                    cancelTimeout();
                    handleWindowResolved(step, window);
                },
                () -> {
                    LOGGER.debug("Window for step '{}' not found. Listening for new windows.", step.title());
                    windowListListener = change -> {
                        while (change.next()) {
                            if (change.wasAdded()) {
                                resolver.resolve().ifPresent(newWindow -> {
                                    LOGGER.debug("Dynamically resolved window for step '{}'", step.title());
                                    cancelTimeout();
                                    if (windowListListener != null) {
                                        Window.getWindows().removeListener(windowListListener);
                                        windowListListener = null;
                                    }
                                    handleWindowResolved(step, newWindow);
                                });
                            }
                        }
                    };
                    Window.getWindows().addListener(windowListListener);
                });
    }

    private void handleWindowResolved(WalkthroughStep step, Window window) {
        this.resolvedWindow = window;
        LOGGER.debug("Window resolved for step '{}': {}", step.title(), window.getClass().getSimpleName());

        windowShowingListener = (_, wasShowing, isShowing) -> {
            if (wasShowing && !isShowing) {
                LOGGER.debug("Window for step '{}' closed. Reverting.", step.title());
                revertToPreviousStep();
            }
        };
        window.showingProperty().addListener(windowShowingListener);

        step.resolver()
            .ifPresentOrElse(
                    resolver -> resolveNode(step, window, resolver),
                    () -> handleNodeResolved(step, window, null));
    }

    private void resolveNode(WalkthroughStep step, Window window, NodeResolver resolver) {
        startTimeout(() -> {
            if (resolvedNode == null) {
                LOGGER.warn("Timeout reached while waiting for node resolution for step '{}'. Reverting.", step.title());
                cleanUp();
                revertToPreviousStep();
            }
        });

        Scene scene = window.getScene();
        if (scene != null) {
            attemptNodeResolutionOnScene(step, window, scene, resolver);
            return;
        }

        LOGGER.debug("Scene for step '{}' not ready. Listening for scene.", step.title());
        sceneListener = (_, _, newScene) -> {
            if (newScene != null) {
                if (sceneListener != null) {
                    window.sceneProperty().removeListener(sceneListener);
                    sceneListener = null;
                }
                attemptNodeResolutionOnScene(step, window, newScene, resolver);
            }
        };
        window.sceneProperty().addListener(sceneListener);
    }

    private void attemptNodeResolutionOnScene(WalkthroughStep step, Window window, Scene scene, NodeResolver resolver) {
        resolver.resolve(scene).ifPresentOrElse(
                node -> handleNodeResolved(step, window, node),
                () -> {
                    LOGGER.debug("Node for step '{}' not found. Listening for scene changes.", step.title());

                    ListChangeListener<Node> childrenListener = _ -> resolver.resolve(scene).ifPresent(foundNode -> {
                        LOGGER.debug("Node found via childrenListener for step '{}'", step.title());
                        if (recursiveChildrenListener != null) {
                            recursiveChildrenListener.detach();
                            recursiveChildrenListener = null;
                        }
                        handleNodeResolved(step, window, foundNode);
                    });

                    recursiveChildrenListener = new RecursiveChildrenListener(childrenListener);
                    recursiveChildrenListener.attachToScene(scene);
                });
    }

    private void handleNodeResolved(WalkthroughStep step, Window window, @Nullable Node node) {
        cancelTimeout();

        this.resolvedNode = node;
        if (node == null) {
            return;
        }
        LOGGER.debug("Node resolved for step '{}': {}", step.title(), node);
        nodeVisibleListener = (_, wasVisible, isVisible) -> {
            if (wasVisible && !isVisible) {
                LOGGER.debug("Node for step '{}' is no longer visible. Reverting.", step.title());
                revertToPreviousStep();
            }
        };
        node.visibleProperty().addListener(nodeVisibleListener);
        display(step, window, node);
    }

    private void display(WalkthroughStep step, Window window, @Nullable Node node) {
        LOGGER.debug("Displaying overlay for step '{}'", step.title());
        walkthroughHighlighter.applyHighlight(step.highlight().orElse(null), window.getScene(), node);
        WindowOverlay overlay = overlays.computeIfAbsent(window, w -> new WindowOverlay(w, walkthrough));
        switch (step) {
            case TooltipStep tooltip ->
                    overlay.showTooltip(tooltip, node, this::stopCheckingRevert);
            case PanelStep panel ->
                    overlay.showPanel(panel, node, this::stopCheckingRevert);
        }
    }

    private void stopCheckingRevert() {
        LOGGER.debug("Stopping revert checks for step: {}, window: {}, node: {}",
                walkthrough.getStepAtIndex(walkthrough.currentStepProperty().get()).title(),
                resolvedWindow != null ? resolvedWindow.getClass().getSimpleName() : "null",
                resolvedNode != null ? resolvedNode.getClass().getSimpleName() : "null");

        if (resolvedWindow != null && windowShowingListener != null) {
            resolvedWindow.showingProperty().removeListener(windowShowingListener);
            windowShowingListener = null;
        }
        if (resolvedNode != null && nodeVisibleListener != null) {
            resolvedNode.visibleProperty().removeListener(nodeVisibleListener);
            nodeVisibleListener = null;
        }
    }

    private void revertToPreviousStep() {
        stopCheckingRevert();
        LOGGER.info("Attempting to revert to previous resolvable step");

        int currentIndex = walkthrough.currentStepProperty().get();

        for (int i = currentIndex - 1; i >= 0; i--) {
            WalkthroughStep previousStep = walkthrough.getStepAtIndex(i);
            Window activeWindow = previousStep.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(stage);
            Scene scene = activeWindow.getScene();
            if (scene != null && (previousStep.resolver().isEmpty() || previousStep.resolver().get().resolve(scene).isPresent())) {
                LOGGER.info("Reverting to step {} from step {}", i, currentIndex);
                walkthrough.goToStep(i);
                return;
            }
        }

        LOGGER.warn("No previous resolvable step found, staying at current step");
    }

    private void startTimeout(Runnable onTimeout) {
        LOGGER.debug("Started timeout");
        cancelTimeout();

        timeoutTransition = new PauseTransition(Duration.millis(RESOLUTION_TIMEOUT_MS));
        timeoutTransition.setOnFinished(_ -> onTimeout.run());
        timeoutTransition.play();
    }

    private void cancelTimeout() {
        LOGGER.debug("Timeout cancelled");
        if (timeoutTransition != null) {
            timeoutTransition.stop();
            timeoutTransition = null;
        }
    }
}
