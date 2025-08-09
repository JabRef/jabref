package org.jabref.gui.walkthrough.utils;

import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.stage.Window;

import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.sideeffect.SideEffectExecutor;
import org.jabref.gui.walkthrough.declarative.sideeffect.WalkthroughSideEffect;
import org.jabref.gui.walkthrough.declarative.step.SideEffect;
import org.jabref.gui.walkthrough.declarative.step.VisibleComponent;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;

import com.sun.javafx.scene.NodeHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughReverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughReverter.class);

    private final Walkthrough walkthrough;
    private final Window fallbackWindow;
    private final SideEffectExecutor sideEffectExecutor;

    private @Nullable ChangeListener<Boolean> windowShowingListener;
    private @Nullable ChangeListener<Boolean> nodeVisibleListener;

    private @Nullable Window resolvedWindow;
    private @Nullable Node resolvedNode;

    public WalkthroughReverter(@NonNull Walkthrough walkthrough,
                               @NonNull Window fallbackWindow,
                               @NonNull SideEffectExecutor sideEffectExecutor) {
        this.walkthrough = walkthrough;
        this.fallbackWindow = fallbackWindow;
        this.sideEffectExecutor = sideEffectExecutor;
    }

    public void attach(@NonNull Window resolvedWindow, @Nullable Node resolvedNode) {
        detach();

        this.resolvedWindow = resolvedWindow;
        this.resolvedNode = resolvedNode;

        windowShowingListener = (_, wasShowing, isShowing) -> {
            if (wasShowing && !isShowing) {
                LOGGER.debug("Window for step '{}' closed. Reverting.", walkthrough.getCurrentStep().title());
                findAndUndo();
            }
        };
        this.resolvedWindow.showingProperty().addListener(windowShowingListener);

        if (this.resolvedNode != null) {
            nodeVisibleListener = (_, wasVisible, isVisible) -> {
                if (wasVisible && !isVisible) {
                    LOGGER.debug("Node for step '{}' is no longer visible. Reverting.", walkthrough.getCurrentStep().title());
                    findAndUndo();
                }
            };
            NodeHelper.treeVisibleProperty(this.resolvedNode).addListener(nodeVisibleListener);
        }
    }

    public void detach() {
        if (resolvedWindow != null && windowShowingListener != null) {
            resolvedWindow.showingProperty().removeListener(windowShowingListener);
        }
        windowShowingListener = null;

        if (resolvedNode != null && nodeVisibleListener != null) {
            NodeHelper.treeVisibleProperty(resolvedNode).removeListener(nodeVisibleListener);
        }
        nodeVisibleListener = null;

        resolvedWindow = null;
        resolvedNode = null;
    }

    public void findAndUndo() {
        detach();
        int currentIndex = walkthrough.currentStepProperty().get();
        undoTo(currentIndex);
    }

    private void undoTo(int from) {
        for (int i = from; i >= 0; i--) {
            WalkthroughStep step = walkthrough.getStepAtIndex(i);
            if (step instanceof SideEffect) {
                undo(i);
            }
        }

        for (int i = from - 1; i >= 0; i--) {
            if (!(walkthrough.getStepAtIndex(i) instanceof VisibleComponent previousStep)) {
                continue;
            }

            Optional<Window> window = previousStep.windowResolver().flatMap(WindowResolver::resolve);
            if (window.isEmpty() && previousStep.windowResolver().isPresent()) {
                continue;
            }
            Window activeWindow = window.orElse(fallbackWindow);
            if (activeWindow.getScene() != null &&
                    (previousStep.nodeResolver().isEmpty() || previousStep.nodeResolver().get().resolve(activeWindow.getScene()).isPresent())) {
                LOGGER.info("Reverting to step {} from step {}", i, from);
                walkthrough.goToStep(i);
                return;
            }
        }

        LOGGER.warn("No previous resolvable step found, quitting walkthrough.");
        walkthrough.quit();
    }

    private void undo(int stepIndex) {
        WalkthroughStep step = walkthrough.getStepAtIndex(stepIndex);
        if (step instanceof SideEffect(String title, WalkthroughSideEffect sideEffect)) {
            if (!sideEffectExecutor.executeBackward(sideEffect, walkthrough)) {
                LOGGER.warn("Failed to revert side effect {}: {}", title, sideEffect.description());
            }
        }
    }

    public void revertAll() {
        LOGGER.debug("Reverting all executed side effects");
        for (int i = walkthrough.currentStepProperty().get(); i >= 0; i--) {
            undo(i);
        }
    }
}
