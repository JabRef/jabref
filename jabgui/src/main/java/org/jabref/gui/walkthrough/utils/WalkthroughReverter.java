package org.jabref.gui.walkthrough.utils;

import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.util.DelayedExecution;
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
    /// The use of delay is related to the following error that can be reproduced as of
    /// [00fc407](https://github.com/JabRef/jabref/tree/00fc407620b3132c0cdea8c65750d0e5616bb597):
    ///
    /// If the "Add Group" walkthrough is launched, the following action is performed:
    /// 1. Launch walkthrough
    /// 2. Create the new group "Research Papers"
    /// 3. In step 8 (i.e., immediately after the new group is created), click on other
    /// group
    /// 4. The reversion occur because the first-entry that are present in the "All
    /// Groups" is not visible
    /// 5. [IndexOutOfBoundsException] is thrown by JavaFX and the walkthrough become
    /// entirely non-responsive.
    ///
    /// It's not clear why such delay fixes this issue, but it's necessary. A possible
    /// hypothesis is that a re-render occurred during the change in the selection state,
    /// which leads to restoration to the default event dispatcher and error due to the
    /// monkey-patched dispatcher isn't perfectly compatible with JavaFX's inner work.
    private static final Duration REVERSION_DELAY = Duration.millis(50);

    private final Walkthrough walkthrough;
    private final Window fallbackWindow;
    private final SideEffectExecutor sideEffectExecutor;

    private @Nullable ChangeListener<Boolean> windowShowingListener;
    private @Nullable ChangeListener<Boolean> nodeVisibleListener;

    private @Nullable Window resolvedWindow;
    private @Nullable Node resolvedNode;
    private @Nullable DelayedExecution reversion;

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
                if (reversion != null) {
                    reversion.cancel();
                }
                reversion = new DelayedExecution(REVERSION_DELAY, this::findAndUndo);
                reversion.start();
            }
        };
        this.resolvedWindow.showingProperty().addListener(windowShowingListener);

        if (this.resolvedNode != null) {
            nodeVisibleListener = (_, wasVisible, isVisible) -> {
                if (wasVisible && !isVisible) {
                    LOGGER.debug("Node for step '{}' is no longer visible. Reverting.", walkthrough.getCurrentStep().title());
                    if (reversion != null) {
                        reversion.cancel();
                    }
                    reversion = new DelayedExecution(REVERSION_DELAY, this::findAndUndo);
                    reversion.start();
                }
            };
            NodeHelper.treeVisibleProperty(this.resolvedNode).addListener(nodeVisibleListener);
        }
    }

    public void detach() {
        if (reversion != null) {
            reversion.cancel();
        }
        reversion = null;

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

    /// Reverts the walkthrough to the last valid step before the given index. This
    /// method assumes that the step in the given index is not valid anymore without
    /// checking whether it's valid.
    private void undoTo(int from) {
        for (int i = from - 1; i >= 0; i--) {
            switch (walkthrough.getStepAtIndex(i)) {
                case VisibleComponent component -> {
                    Optional<Window> window = component.windowResolver().flatMap(WindowResolver::resolve);
                    if (window.isEmpty() && component.windowResolver().isPresent()) {
                        continue;
                    }
                    Window activeWindow = window.orElse(fallbackWindow);
                    if (activeWindow.getScene() != null &&
                            (component.nodeResolver().isEmpty() || component.nodeResolver().get().resolve(activeWindow.getScene()).isPresent())) {
                        LOGGER.info("Reverting to step {} from step {}", i, from);
                        walkthrough.goToStep(i);
                        return;
                    }
                }
                case SideEffect _ ->
                        undo(i);
            }
        }

        LOGGER.warn("No previous resolvable step found, quitting walkthrough.");
        walkthrough.quit();
    }

    private void undo(int stepIndex) {
        WalkthroughStep step = walkthrough.getStepAtIndex(stepIndex);
        if (step instanceof SideEffect(
                String title,
                WalkthroughSideEffect sideEffect
        )) {
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
