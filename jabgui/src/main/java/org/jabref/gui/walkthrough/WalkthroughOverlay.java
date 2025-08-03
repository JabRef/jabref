package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

/// Overlay to display, render, highlight, and mutate walkthroughs.
///
/// @implNote This class is only thread-safe for its intended use case. For each
/// walkthrough, the following occurs:
///
/// 1. [#show(WalkthroughStep)] is called
/// 2. Attempt to resolve the node using [WalkthroughResolver]. Upon completion, at most
/// one call is made to [#displayWalkthroughStep(WalkthroughResult)] or
/// [#revertToPreviousStep()].
///     1. For [#revertToPreviousStep()], no concurrency is involved (note that no
/// listeners that could trigger revert are attached yet). Back to case 1.
///     2. For [#displayWalkthroughStep(WalkthroughResult)]
///         1. We start listening for potential revert, creating
/// [#revertToPreviousStep()] trying to revert at the same time, which is secured to
/// only accept the first call and ignore all the rest of the calls from property
/// changes. Back to case 2.1.
///         2. We start listening for when the next step of navigation can occur, see
/// [WindowOverlay], which could lead to at most one call to [#prepareForNavigation()]
/// first, and then another call to [#show(WalkthroughStep)] (as a result of
/// state-change in the [Walkthrough]) (see
/// [org.jabref.gui.walkthrough.declarative.NavigationPredicate]'s guard rail on
/// preventing multiple calls). In the extreme case where [#show(WalkthroughStep)]
/// occurs at the same time as [#displayWalkthroughStep(WalkthroughResult)], we have
/// synchronized them, preventing failure to keep track of overlays and listeners. Back
/// to case 1.
///         3. We also could be [#detachAll()], see [#showQuitConfirmationAndQuit()]
/// which is applied to the highlight effects. Consider when a user clicks on the
/// highlight to quit when the highlight is applied and [WindowOverlay] is not added to
/// the pane yet, we now have [#detachAll()] and
/// [#displayWalkthroughStep(WalkthroughResult)] at the same time. We would not have
/// properly cleaned up since [#overlays], [#windowShowingListener], and
/// [#nodeVisibleListener] have yet to be registered, so we `synchronized` them.
///
/// In most sane cases, no concurrency issue would occur unless the user clicks at some
/// extremely fast speed.
public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);

    private final Map<Window, WindowOverlay> overlays = new HashMap<>();
    private final Stage stage;
    private final Walkthrough walkthrough;
    private final WalkthroughHighlighter walkthroughHighlighter;
    private final WalkthroughScroller walkthroughScroller;
    private final SideEffectExecutor sideEffectExecutor;
    private final Map<Integer, WalkthroughSideEffect> executedSideEffects = new HashMap<>();

    private @Nullable WalkthroughResolver resolver;
    private @Nullable ChangeListener<Boolean> windowShowingListener;
    private @Nullable ChangeListener<Boolean> nodeVisibleListener;

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

    public synchronized void show(@NonNull WalkthroughStep step) {
        LOGGER.debug("Showing step: {}", step.title());
        cleanUp();

        switch (step) {
            case SideEffect(String title, WalkthroughSideEffect sideEffect) -> {
                int currentStepIndex = walkthrough.currentStepProperty().get();
                LOGGER.debug("Executing side effect for step: {}", title);

                if (sideEffectExecutor.executeForward(sideEffect, walkthrough)) {
                    executedSideEffects.put(currentStepIndex, sideEffect);
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
                        component.nodeResolver().orElse(null),
                        this::handleResolutionResult);
                resolver.startResolution();
            }
        }
    }

    public synchronized void detachAll() {
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

    private synchronized void displayWalkthroughStep(WalkthroughResult result) {
        Optional<Window> window = result.window();
        if (window.isEmpty()) {
            throw new IllegalStateException("Resolution should not be successful without Window being resolved.");
        }
        this.resolvedWindow = window.get();
        this.resolvedNode = result.node().orElse(null);
        VisibleComponent component = (VisibleComponent) walkthrough.getCurrentStep();

        LOGGER.debug("Displaying overlay for component '{}'", component.title());

        if (resolvedNode != null) {
            walkthroughScroller.setup(resolvedNode);
        }
        walkthroughHighlighter.applyHighlight(
                component.highlight().orElse(null),
                resolvedWindow.getScene(),
                resolvedNode);
        WindowOverlay overlay = overlays.computeIfAbsent(resolvedWindow,
                w -> new WindowOverlay(w, WalkthroughPane.getInstance(w), walkthrough));

        switch (component) {
            case TooltipStep tooltip ->
                    overlay.showTooltip(tooltip, resolvedNode, this::prepareForNavigation);
            case PanelStep panel ->
                    overlay.showPanel(panel, resolvedNode, this::prepareForNavigation);
        }

        setupRevertListeners(component);
    }

    private void setupRevertListeners(VisibleComponent component) {
        Runnable reverter = WalkthroughUtils.once(this::revertToPreviousStep);

        windowShowingListener = (_, wasShowing, isShowing) -> {
            if (wasShowing && !isShowing) {
                LOGGER.debug("Window for step '{}' closed. Reverting.", component.title());
                reverter.run();
            }
        };
        resolvedWindow.showingProperty().addListener(windowShowingListener);

        if (resolvedNode != null) {
            nodeVisibleListener = (_, wasVisible, isVisible) -> {
                if (wasVisible && !isVisible) {
                    LOGGER.debug("Node for step '{}' is no longer visible. Reverting.", component.title());
                    reverter.run();
                }
            };
            resolvedNode.visibleProperty().addListener(nodeVisibleListener);
        }
    }

    private void revertSideEffect(@Nullable WalkthroughSideEffect sideEffect) {
        if (sideEffect == null) {
            return;
        }
        if (!sideEffectExecutor.executeBackward(sideEffect, walkthrough)) {
            LOGGER.warn("Failed to revert side effect: {}", sideEffect.description());
        }
    }

    private void revertAllSideEffects() {
        LOGGER.debug("Reverting all executed side effects");
        List<Integer> stepIndices = new ArrayList<>(executedSideEffects.keySet());
        stepIndices.sort(Integer::compareTo);
        for (int i = stepIndices.size() - 1; i >= 0; i--) {
            Integer stepIndex = stepIndices.get(i);
            revertSideEffect(executedSideEffects.get(stepIndex));
        }
        executedSideEffects.clear();
    }

    private void revertToPreviousStep() {
        stopRevertListeners();
        LOGGER.info("Attempting to revert to previous resolvable step");
        int currentIndex = walkthrough.currentStepProperty().get();
        revertSideEffect(executedSideEffects.remove(currentIndex));

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
            if (scene != null && (previousStep.nodeResolver().isEmpty() || previousStep.nodeResolver().get().resolve(scene).isPresent())) {
                LOGGER.info("Reverting to step {} from step {}", i, currentIndex);
                for (int j = currentIndex - 1; j > i; j--) {
                    revertSideEffect(executedSideEffects.remove(j));
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

        overlays.values().forEach(WindowOverlay::hide);
    }

    private void prepareForNavigation() {
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
