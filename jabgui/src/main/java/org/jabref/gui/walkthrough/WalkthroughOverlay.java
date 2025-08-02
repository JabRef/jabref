package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.DialogService;
import org.jabref.gui.walkthrough.WalkthroughResolver.WalkthroughResult;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.sideeffect.SideEffectExecutor;
import org.jabref.gui.walkthrough.declarative.sideeffect.WalkthroughSideEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.SideEffect;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.VisibleComponent;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);

    private enum WalkthroughPhase {
        INACTIVE,
        RESOLVING,
        DISPLAYING,
        NAVIGATING
    }

    private final Map<Window, WindowOverlay> overlays = new HashMap<>();
    private final Stage stage;
    private final Walkthrough walkthrough;
    private final WalkthroughHighlighter walkthroughHighlighter;
    private final WalkthroughScroller walkthroughScroller;
    private final SideEffectExecutor sideEffectExecutor;
    private final Map<Integer, List<WalkthroughSideEffect>> executedSideEffects = new ConcurrentHashMap<>();

    private @Nullable WalkthroughResolver resolver;
    private @Nullable ChangeListener<Boolean> windowShowingListener;
    private @Nullable ChangeListener<Boolean> nodeVisibleListener;

    private volatile WalkthroughPhase phase = WalkthroughPhase.INACTIVE;
    private Window resolvedWindow;
    private Node resolvedNode;

    public WalkthroughOverlay(Stage stage, Walkthrough walkthrough) {
        this.stage = stage;
        this.walkthrough = walkthrough;
        this.walkthroughHighlighter = new WalkthroughHighlighter();
        this.walkthroughScroller = new WalkthroughScroller();
        this.sideEffectExecutor = new SideEffectExecutor();
        this.walkthroughHighlighter.setOnBackgroundClick(this::showQuitConfirmationAndQuit);
    }

    public void show(@NonNull WalkthroughStep step) {
        LOGGER.debug("Showing step: {}", step.title());
        cleanUp();
        phase = WalkthroughPhase.RESOLVING;

        switch (step) {
            case SideEffect(String title, WalkthroughSideEffect sideEffect) -> {
                int currentStepIndex = walkthrough.currentStepProperty().get();
                LOGGER.debug("Executing side effect for step: {}", title);

                if (sideEffectExecutor.executeForward(sideEffect, walkthrough)) {
                    executedSideEffects.computeIfAbsent(currentStepIndex, _ -> new ArrayList<>())
                                       .add(sideEffect);
                    walkthrough.nextStep();
                } else {
                    LOGGER.error("Failed to execute side effect: {}", sideEffect.description());
                    LOGGER.warn("Side effect failed for step: {}", title);
                    revertToPreviousStep();
                }
            }
            case VisibleComponent component -> {
                WindowResolver windowResolver = component.windowResolver()
                                                         .orElse(() -> Optional.of(stage));

                resolver = new WalkthroughResolver(
                        windowResolver,
                        component.resolver().orElse(null),
                        this::handleResolutionResult);
                resolver.startResolution();
            }
        }
    }

    public void detachAll() {
        cleanUp();
        revertAllSideEffects();

        walkthroughHighlighter.detachAll();
        overlays.values().forEach(WindowOverlay::detach);
        overlays.clear();
    }

    public void showQuitConfirmationAndQuit() {
        overlays.values().forEach(WindowOverlay::hide);

        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        boolean shouldQuit = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Quit walkthrough"),
                Localization.lang("Are you sure you want to quit the walkthrough?"),
                Localization.lang("Quit walkthrough"),
                Localization.lang("Continue walkthrough"));

        if (shouldQuit) {
            walkthrough.quit();
        } else {
            show(walkthrough.getCurrentStep());
        }
    }

    private void handleResolutionResult(WalkthroughResult result) {
        if (result.wasSuccessful()) {
            displayWalkthroughStep(result);
        } else {
            LOGGER.error("Failed to resolve node for step '{}'. Reverting.", walkthrough.getCurrentStep().title());
            revertToPreviousStep();
        }
        resolver = null;
    }

    private void displayWalkthroughStep(WalkthroughResult result) {
        Optional<Window> window = result.window();
        if (window.isEmpty()) {
            LOGGER.error("Resolution succeeded but window is missing for component '{}'", walkthrough.getCurrentStep().title());
            revertToPreviousStep();
            return;
        }
        this.resolvedWindow = window.get();
        this.resolvedNode = result.node().orElse(null);
        VisibleComponent component = (VisibleComponent) walkthrough.getCurrentStep();
        phase = WalkthroughPhase.DISPLAYING;

        LOGGER.debug("Displaying overlay for component '{}'", component.title());

        setupRevertListeners(component);

        if (resolvedNode != null) {
            walkthroughScroller.setup(resolvedNode);
        }
        walkthroughHighlighter.applyHighlight(component.highlight().orElse(null),
                resolvedWindow.getScene(), resolvedNode);

        WindowOverlay overlay = overlays.computeIfAbsent(resolvedWindow,
                w -> new WindowOverlay(w, WalkthroughPane.getInstance(w), walkthrough));

        switch (component) {
            case TooltipStep tooltip ->
                    overlay.showTooltip(tooltip, resolvedNode, this::prepareForNavigation);
            case PanelStep panel ->
                    overlay.showPanel(panel, resolvedNode, this::prepareForNavigation);
        }
    }

    private void setupRevertListeners(VisibleComponent component) {
        windowShowingListener = (_, wasShowing, isShowing) -> {
            if (wasShowing && !isShowing) {
                LOGGER.debug("Window for step '{}' closed. Reverting.", component.title());
                revertToPreviousStep();
            }
        };
        resolvedWindow.showingProperty().addListener(windowShowingListener);

        if (resolvedNode != null) {
            nodeVisibleListener = (_, wasVisible, isVisible) -> {
                if (wasVisible && !isVisible) {
                    LOGGER.debug("Node for step '{}' is no longer visible. Reverting.", component.title());
                    revertToPreviousStep();
                }
            };
            resolvedNode.visibleProperty().addListener(nodeVisibleListener);
        }
    }

    private void revertSideEffects(@NonNull List<WalkthroughSideEffect> sideEffectsToRevert) {
        LOGGER.debug("Reverting {} side effects", sideEffectsToRevert.size());
        for (int i = sideEffectsToRevert.size() - 1; i >= 0; i--) {
            WalkthroughSideEffect sideEffect = sideEffectsToRevert.get(i);
            if (!sideEffectExecutor.executeBackward(sideEffect, walkthrough)) {
                LOGGER.warn("Failed to revert side effect: {}", sideEffect.description());
            }
        }
    }

    private void revertAllSideEffects() {
        LOGGER.debug("Reverting all executed side effects");
        List<Integer> stepIndices = new ArrayList<>(executedSideEffects.keySet());
        stepIndices.sort(Integer::compareTo);
        for (int i = stepIndices.size() - 1; i >= 0; i--) {
            Integer stepIndex = stepIndices.get(i);
            List<WalkthroughSideEffect> stepSideEffects = executedSideEffects.get(stepIndex);
            if (stepSideEffects != null) {
                revertSideEffects(stepSideEffects);
            }
        }
        executedSideEffects.clear();
    }

    private void revertToPreviousStep() {
        synchronized (this) {
            if (phase != WalkthroughPhase.DISPLAYING) {
                return;
            }
            phase = WalkthroughPhase.NAVIGATING;
        }

        stopRevertListeners();
        LOGGER.info("Attempting to revert to previous resolvable step");
        int currentIndex = walkthrough.currentStepProperty().get();

        List<WalkthroughSideEffect> currentStepSideEffects = executedSideEffects.remove(currentIndex);
        if (currentStepSideEffects != null) {
            revertSideEffects(currentStepSideEffects);
        }

        for (int i = currentIndex - 1; i >= 0; i--) {
            if (!(walkthrough.getStepAtIndex(i) instanceof VisibleComponent previousStep)) {
                continue;
            }

            Optional<Window> window = previousStep.windowResolver().flatMap(WindowResolver::resolve);
            if (window.isEmpty() && previousStep.windowResolver().isPresent()) {
                continue;
            }
            Window activeWindow = window.orElse(stage);
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

        LOGGER.warn("No previous resolvable step found, quitting walkthrough.");
        walkthrough.quit();
    }

    private void cleanUp() {
        LOGGER.debug("Cleaning up listeners and state for the previous step");
        if (resolver != null) {
            resolver.cancel();
            resolver = null;
        }

        stopRevertListeners();
        walkthroughScroller.cleanup();

        resolvedWindow = null;
        resolvedNode = null;
        phase = WalkthroughPhase.INACTIVE;

        overlays.values().forEach(WindowOverlay::hide);
    }

    private void prepareForNavigation() {
        this.phase = WalkthroughPhase.NAVIGATING;
        stopRevertListeners();
    }

    private void stopRevertListeners() {
        if (resolvedWindow != null && windowShowingListener != null) {
            resolvedWindow.showingProperty().removeListener(windowShowingListener);
            windowShowingListener = null;
        }
        if (resolvedNode != null && nodeVisibleListener != null) {
            resolvedNode.visibleProperty().removeListener(nodeVisibleListener);
            nodeVisibleListener = null;
        }
    }
}
