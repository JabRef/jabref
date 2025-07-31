package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
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
import org.jabref.gui.walkthrough.declarative.sideeffect.SideEffectExecutor;
import org.jabref.gui.walkthrough.declarative.sideeffect.WalkthroughSideEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.SideEffectStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.VisibleWalkthroughStep;
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
    private final Map<Window, WalkthroughPane> panes = new HashMap<>();
    private final Stage stage;
    private final WalkthroughHighlighter walkthroughHighlighter;
    private final Walkthrough walkthrough;
    private final SideEffectExecutor sideEffectExecutor;

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

    private final Map<Integer, List<WalkthroughSideEffect>> executedSideEffects = new HashMap<>();
    /// Flag to prevent unwanted reverts while transitioning between steps
    ///
    /// @implNote Consider a race condition between
    /// [WalkthroughOverlay#windowShowingListener] and
    /// [WalkthroughOverlay#stopCheckingRevert()]. We cannot possibly block a
    /// ContextMenu from closing just by monkey patch its item's
    /// [javafx.event.EventDispatcher] as in
    /// [org.jabref.gui.walkthrough.declarative.NavigationPredicate]. Therefore, a
    /// potential race condition exists where the window listener might be triggered
    /// before we actually cancelled this listener, leading to an unwanted revert.
    private final AtomicBoolean isActivelyShowing = new AtomicBoolean(false);

    public WalkthroughOverlay(Stage stage, Walkthrough walkthrough) {
        this.stage = stage;
        this.walkthrough = walkthrough;
        this.walkthroughHighlighter = new WalkthroughHighlighter(this::getOrCreateWalkthroughPane);
        this.walkthroughHighlighter.setOnBackgroundClick(this::showQuitConfirmationAndQuit);
        this.sideEffectExecutor = new SideEffectExecutor();
    }

    public void show(@NonNull WalkthroughStep step) {
        LOGGER.debug("Showing step: {}", step.title());
        isActivelyShowing.set(true);
        cleanUp();
        overlays.values().forEach(WindowOverlay::hide);

        switch (step) {
            case SideEffectStep sideEffectStep -> {
                if (executeSideEffect(sideEffectStep)) {
                    walkthrough.nextStep();
                } else {
                    LOGGER.warn("Side effect failed for step: {}", step.title());
                    revertToPreviousStep();
                }
            }
            case VisibleWalkthroughStep visibleStep ->
                    visibleStep.activeWindowResolver()
                               .ifPresentOrElse(
                                       resolver -> resolveWindow(visibleStep, resolver),
                                       () -> handleWindowResolved(visibleStep, stage));
        }
    }

    /// Detaches all overlays, restore the state, and clean up listeners.
    public void detachAll() {
        cleanUp();
        revertAllSideEffects();

        walkthroughHighlighter.detachAll();
        overlays.values().forEach(WindowOverlay::detach);
        overlays.clear();

        panes.values().forEach(WalkthroughPane::detach);
        panes.clear();
    }

    /// Reverts all executed side effects.
    private void revertAllSideEffects() {
        LOGGER.debug("Reverting all executed side effects");

        List<Integer> stepIndices = new ArrayList<>(executedSideEffects.keySet());
        stepIndices.sort((a, b) -> Integer.compare(b, a));

        for (Integer stepIndex : stepIndices) {
            List<WalkthroughSideEffect> stepSideEffects = executedSideEffects.get(stepIndex);
            if (stepSideEffects != null) {
                revertSideEffects(stepSideEffects);
            }
        }

        executedSideEffects.clear();
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

        detachChildrenListener();

        if (resolvedNode != null) {
            if (nodeVisibleListener != null) {
                resolvedNode.visibleProperty().removeListener(nodeVisibleListener);
                nodeVisibleListener = null;
            }
        }

        resolvedWindow = null;
        resolvedNode = null;
    }

    private void resolveWindow(VisibleWalkthroughStep step, WindowResolver resolver) {
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
                    AtomicBoolean noExecution = new AtomicBoolean(false);
                    windowListListener = change -> {
                        if (!noExecution.compareAndSet(false, true)) {
                            return; // No concurrent resolutions
                        }
                        while (change.next()) {
                            if (change.wasAdded()) {
                                resolver.resolve().ifPresentOrElse(
                                        newWindow -> {
                                            LOGGER.debug("Dynamically resolved window for step '{}'", step.title());
                                            cancelTimeout();
                                            if (windowListListener != null) {
                                                Window.getWindows().removeListener(windowListListener);
                                                windowListListener = null;
                                            }
                                            handleWindowResolved(step, newWindow);
                                        },
                                        () -> {
                                            LOGGER.debug("Still no window found for step '{}', continuing to listen for new windows.", step.title());
                                            noExecution.set(false); // Reset for next execution
                                        }
                                );
                            }
                        }
                    };
                    Window.getWindows().addListener(windowListListener);
                });
    }

    private void handleWindowResolved(VisibleWalkthroughStep step, Window window) {
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

    private void resolveNode(VisibleWalkthroughStep step, Window window, NodeResolver resolver) {
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
        AtomicBoolean noExecution = new AtomicBoolean(false);
        sceneListener = (_, _, newScene) -> {
            if (!noExecution.compareAndSet(false, true)) {
                return; // No concurrent resolutions
            }

            if (newScene == null) {
                LOGGER.debug("Scene for step '{}' is still null, continuing to listen for scene changes.", step.title());
                noExecution.set(false); // Reset for next execution
                return;
            }

            if (sceneListener != null) {
                window.sceneProperty().removeListener(sceneListener);
                sceneListener = null;
            }
            attemptNodeResolutionOnScene(step, window, newScene, resolver);
        };
        window.sceneProperty().addListener(sceneListener);
    }

    private void attemptNodeResolutionOnScene(VisibleWalkthroughStep step, Window window, Scene scene, NodeResolver resolver) {
        resolver.resolve(scene)
                .ifPresentOrElse(
                        node -> handleNodeResolved(step, window, node),
                        () -> {
                            LOGGER.debug("Node for step '{}' not found. Listening for scene changes.", step.title());
                            AtomicBoolean noExecution = new AtomicBoolean(false);

                            InvalidationListener childrenListener = _ -> {
                                if (!noExecution.compareAndSet(false, true)) {
                                    return; // No concurrent resolutions
                                }

                                resolver.resolve(scene).ifPresentOrElse(
                                        foundNode -> {
                                            LOGGER.debug("Node found via childrenListener for step '{}'", step.title());
                                            detachChildrenListener();
                                            handleNodeResolved(step, window, foundNode);
                                            // No need to reset noExecution. Block all further executions.
                                        },
                                        () -> {
                                            LOGGER.debug("Node still not found for step '{}', continuing to listen for changes.", step.title());
                                            noExecution.set(false); // Reset for next execution
                                        });
                            };

                            recursiveChildrenListener = new RecursiveChildrenListener(childrenListener);
                            recursiveChildrenListener.attachToScene(scene);
                        });
    }

    private void detachChildrenListener() {
        if (recursiveChildrenListener != null) {
            recursiveChildrenListener.detach();
            recursiveChildrenListener = null;
        }
    }

    private void handleNodeResolved(VisibleWalkthroughStep step, Window window, @Nullable Node node) {
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

    private void display(VisibleWalkthroughStep step, Window window, @Nullable Node node) {
        LOGGER.debug("Displaying overlay for step '{}'", step.title());
        walkthroughHighlighter.applyHighlight(step.highlight().orElse(null), window.getScene(), node);
        WindowOverlay overlay = overlays.computeIfAbsent(window, w -> new WindowOverlay(w, getOrCreateWalkthroughPane(w), walkthrough));
        switch (step) {
            case TooltipStep tooltip ->
                    overlay.showTooltip(tooltip, node, this::stopCheckingRevert);
            case PanelStep panel ->
                    overlay.showPanel(panel, node, this::stopCheckingRevert);
        }
    }

    private void stopCheckingRevert() {
        isActivelyShowing.set(false);
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

    private boolean executeSideEffect(@NonNull SideEffectStep sideEffectStep) {
        int currentStepIndex = walkthrough.currentStepProperty().get();
        WalkthroughSideEffect sideEffect = sideEffectStep.sideEffect();

        LOGGER.debug("Executing side effect for step: {}", sideEffectStep.title());

        boolean success = sideEffectExecutor.executeForward(sideEffect, walkthrough);
        if (success) {
            LOGGER.debug("Successfully executed side effect: {}", sideEffect.description());
            executedSideEffects.put(currentStepIndex, List.of(sideEffect));
            return true;
        } else {
            LOGGER.error("Failed to execute side effect: {}", sideEffect.description());
            return false;
        }
    }

    private void revertSideEffects(@NonNull List<WalkthroughSideEffect> sideEffectsToRevert) {
        LOGGER.debug("Reverting {} side effects", sideEffectsToRevert.size());

        for (int i = sideEffectsToRevert.size() - 1; i >= 0; i--) {
            WalkthroughSideEffect sideEffect = sideEffectsToRevert.get(i);
            boolean success = sideEffectExecutor.executeBackward(sideEffect, walkthrough);
            if (success) {
                LOGGER.debug("Successfully reverted side effect: {}", sideEffect.description());
            } else {
                LOGGER.warn("Failed to revert side effect: {}", sideEffect.description());
            }
        }
    }

    private void revertToPreviousStep() {
        if (!isActivelyShowing.get()) {
            return;
        }

        stopCheckingRevert();
        LOGGER.info("Attempting to revert to previous resolvable step");

        int currentIndex = walkthrough.currentStepProperty().get();

        List<WalkthroughSideEffect> currentStepSideEffects = executedSideEffects.remove(currentIndex);
        if (currentStepSideEffects != null) {
            revertSideEffects(currentStepSideEffects);
        }

        for (int i = currentIndex - 1; i >= 0; i--) {
            if (!(walkthrough.getStepAtIndex(i) instanceof VisibleWalkthroughStep previousStep)) {
                // Skip non-visible steps
                continue;
            }
            Window activeWindow = previousStep.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(stage);
            Scene scene = activeWindow.getScene();
            if (scene != null && (previousStep.resolver().isEmpty() || previousStep.resolver().get().resolve(scene).isPresent())) {
                LOGGER.info("Reverting to step {} from step {}", i, currentIndex);

                for (int j = currentIndex - 1; j > i; j--) {
                    List<WalkthroughSideEffect> stepSideEffects = executedSideEffects.remove(j);
                    if (stepSideEffects != null) {
                        revertSideEffects(stepSideEffects);
                    }
                }

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

    private WalkthroughPane getOrCreateWalkthroughPane(@NonNull Window window) {
        return panes.computeIfAbsent(window, WalkthroughPane::new);
    }
}
