package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.DialogService;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.sideeffect.SideEffectExecutor;
import org.jabref.gui.walkthrough.declarative.sideeffect.WalkthroughSideEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.SideEffect;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.VisibleComponent;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
import org.jabref.gui.walkthrough.utils.WalkthroughResolver;
import org.jabref.gui.walkthrough.utils.WalkthroughResolver.WalkthroughResult;
import org.jabref.gui.walkthrough.utils.WalkthroughReverter;
import org.jabref.gui.walkthrough.utils.WalkthroughScroller;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);

    private final Map<Window, WindowOverlay> overlays = new HashMap<>();
    private final Stage stage;
    private final Walkthrough walkthrough;
    private final WalkthroughHighlighter highlighter;
    private final WalkthroughReverter reverter;
    private final SideEffectExecutor sideEffectExecutor;

    private @Nullable WalkthroughScroller scroller;
    private @Nullable WalkthroughResolver resolver;

    private Window resolvedWindow;
    private Node resolvedNode;

    public WalkthroughOverlay(Stage stage, Walkthrough walkthrough) {
        this.stage = stage;
        this.walkthrough = walkthrough;
        this.highlighter = new WalkthroughHighlighter();
        this.highlighter.setOnBackgroundClick(this::showQuitConfirmationAndQuit);
        this.sideEffectExecutor = new SideEffectExecutor();
        this.reverter = new WalkthroughReverter(walkthrough, stage, sideEffectExecutor);
    }

    public void show(@NonNull WalkthroughStep step) {
        LOGGER.debug("Showing step: {}", step.title());
        cleanUp();

        switch (step) {
            case SideEffect(
                    String title,
                    WalkthroughSideEffect sideEffect
            ) -> {
                LOGGER.debug("Executing side effect for step: {}", title);

                if (sideEffectExecutor.executeForward(sideEffect, walkthrough)) {
                    walkthrough.nextStep();
                } else {
                    LOGGER.error("Failed to execute side effect: {}", sideEffect.description());
                    LOGGER.warn("Side effect failed for step: {}", title);
                    reverter.findAndUndo();
                }
            }
            case VisibleComponent component -> {
                WindowResolver windowResolver = component.windowResolver().orElse(() -> Optional.of(stage));
                resolver = new WalkthroughResolver(
                        windowResolver,
                        component.nodeResolver().orElse(null),
                        this::handleResolutionResult);
                resolver.startResolution();
            }
        }
    }

    public void detachAll() {
        cleanUp();
        reverter.revertAll();
        highlighter.detachAll();
        overlays.values().forEach(WindowOverlay::detach);
        overlays.clear();
    }

    public void showQuitConfirmationAndQuit() {
        hide();
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

    private void hide() {
        overlays.values().forEach(WindowOverlay::hide);
    }

    private void handleResolutionResult(WalkthroughResult result) {
        if (result.wasSuccessful()) {
            displayWalkthroughStep(result);
        } else {
            LOGGER.error("Failed to resolve node for step '{}'. Reverting.", walkthrough.getCurrentStep().title());
            reverter.findAndUndo();
        }
        resolver = null;
    }

    private void displayWalkthroughStep(WalkthroughResult result) {
        Optional<Window> window = result.window();
        if (window.isEmpty()) {
            throw new IllegalStateException("Resolution should not be successful without Window being resolved.");
        }
        this.resolvedWindow = window.get();
        this.resolvedNode = result.node().orElse(null);
        VisibleComponent component = (VisibleComponent) walkthrough.getCurrentStep();

        LOGGER.debug("Displaying overlay for component '{}'", component.title());

        if (resolvedNode != null) {
            this.scroller = new WalkthroughScroller(resolvedNode);
        }

        highlighter.applyHighlight(
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

        reverter.attach(resolvedWindow, resolvedNode);
    }

    private void cleanUp() {
        LOGGER.debug("Cleaning up listeners and state for the previous step");
        if (resolver != null) {
            resolver.cancel();
            resolver = null;
        }

        reverter.detach();
        if (scroller != null) {
            scroller.cleanup();
            scroller = null;
        }

        resolvedWindow = null;
        resolvedNode = null;

        hide();
    }

    /// Called before the Navigation to next step occur.
    ///
    /// 1. Note sometimes node disappear when you click on it (even it's window), consider a menu button on a context
    /// menu. This lead to moving to next step results in revert to previous step.
    /// 2. Hide is necessary because [WindowOverlay] have a hack---automatically recover PopOver throughout the course
    /// when it's supposed to be displayed. That seems harmless, but since we used
    /// [org.jabref.gui.walkthrough.utils.WalkthroughUtils#debounced(Runnable, long)], the following "race condition"
    /// can occur:
    ///     - [#prepareForNavigation()] ran
    ///     - node hid (original event dispatcher ran)
    ///     - popover hid
    ///     - popover restoration scheduled in next 50ms
    ///     - timeout/delayed execution of walkthrough logic started and finished in 50ms, set popover to hide
    ///     - new popover show, ALONG WITH the 50ms delayed one -> two popover at once.
    ///
    /// So the current hack is we will hide [WindowOverlay] so that they won't get hacked restored.
    private void prepareForNavigation() {
        reverter.detach();
        hide();
    }
}
