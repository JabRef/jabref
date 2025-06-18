package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages walkthrough overlays and highlights across multiple windows.
 */
public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);

    private final Map<Window, SingleWindowWalkthroughOverlay> overlays = new HashMap<>();
    private final Stage mainStage;
    private final WalkthroughHighlighter walkthroughHighlighter;
    private final Walkthrough walkthrough;
    private Timeline nodePollingTimeline;

    public WalkthroughOverlay(Stage mainStage, Walkthrough walkthrough) {
        this.mainStage = mainStage;
        this.walkthrough = walkthrough;
        this.walkthroughHighlighter = new WalkthroughHighlighter();
    }

    public void displayStep(@NonNull WalkthroughStep step) {
        overlays.values().forEach(SingleWindowWalkthroughOverlay::hide);
        Window activeWindow = step.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(mainStage);
        Scene scene = activeWindow.getScene();

        Optional<Node> targetNode = step.resolver().flatMap(resolver -> resolver.resolve(scene));
        if (step.resolver().isPresent() && targetNode.isEmpty()) {
            if (step.autoFallback()) {
                tryRevertToPreviousResolvableStep();
            } else {
                startNodePolling(step, activeWindow);
            }
            return;
        }

        walkthroughHighlighter.applyHighlight(scene, step.highlight().orElse(null), targetNode.orElse(null));
        SingleWindowWalkthroughOverlay overlay = getOrCreateOverlay(activeWindow);
        overlay.displayStep(step, targetNode.orElse(null), walkthrough);
    }

    public void detachAll() {
        stopNodePolling();
        walkthroughHighlighter.detachAll();
        overlays.values().forEach(SingleWindowWalkthroughOverlay::detach);
        overlays.clear();
    }

    private void tryRevertToPreviousResolvableStep() {
        LOGGER.info("Attempting to revert to previous resolvable step");

        int currentIndex = walkthrough.currentStepProperty().get();
        for (int i = currentIndex - 1; i >= 0; i--) {
            WalkthroughStep previousStep = getStepAtIndex(i);
            Window activeWindow = previousStep.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(mainStage);
            Scene scene = activeWindow.getScene();
            if (scene != null && (previousStep.resolver().isEmpty() || previousStep.resolver().get().resolve(scene).isPresent())) {
                LOGGER.info("Reverting to step {} from step {}", i, currentIndex);
                walkthrough.goToStep(i);
                return;
            }
        }

        LOGGER.warn("No previous resolvable step found, staying at current step");
    }

    private void startNodePolling(WalkthroughStep step, Window activeWindow) {
        LOGGER.info("Auto-fallback disabled for step: {}, starting node polling", step.title());
        stopNodePolling();

        nodePollingTimeline = new Timeline(new KeyFrame(Duration.millis(100), _ -> {
            Scene scene = activeWindow.getScene();
            if (scene == null) {
                return;
            }
            Optional<Node> targetNode = step.resolver().flatMap(resolver -> resolver.resolve(scene));
            if (targetNode.isEmpty()) {
                return;
            }

            LOGGER.info("Target node found for step: {}, displaying step", step.title());
            stopNodePolling();

            walkthroughHighlighter.applyHighlight(scene, step.highlight().orElse(null), targetNode.orElse(null));
            SingleWindowWalkthroughOverlay overlay = getOrCreateOverlay(activeWindow);
            overlay.displayStep(step, targetNode.get(), walkthrough);
        }));

        nodePollingTimeline.setCycleCount(Timeline.INDEFINITE);
        nodePollingTimeline.play();
    }

    private void stopNodePolling() {
        if (nodePollingTimeline != null) {
            nodePollingTimeline.stop();
            nodePollingTimeline = null;
        }
    }

    private @NonNull WalkthroughStep getStepAtIndex(int index) {
        return walkthrough.getSteps().get(index);
    }

    private SingleWindowWalkthroughOverlay getOrCreateOverlay(Window window) {
        return overlays.computeIfAbsent(window, SingleWindowWalkthroughOverlay::new);
    }
}
