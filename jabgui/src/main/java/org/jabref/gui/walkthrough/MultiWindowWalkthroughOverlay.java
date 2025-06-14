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
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages walkthrough overlays and highlights across multiple windows.
 */
public class MultiWindowWalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiWindowWalkthroughOverlay.class);

    private final Map<Window, SingleWindowOverlay> overlays = new HashMap<>();
    private final Stage mainStage;
    private final HighlightManager highlightManager;
    private final Walkthrough walkthrough;
    private Timeline nodePollingTimeline;

    public MultiWindowWalkthroughOverlay(Stage mainStage, Walkthrough walkthrough) {
        this.mainStage = mainStage;
        this.walkthrough = walkthrough;
        this.highlightManager = new HighlightManager();
    }

    public void displayStep(@NonNull WalkthroughNode step) {
        overlays.values().forEach(SingleWindowOverlay::hide);
        Window activeWindow = step.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(mainStage);
        Scene scene = activeWindow.getScene();

        Optional<Node> targetNode = step.resolver().resolve(scene);
        if (targetNode.isEmpty()) {
            if (step.autoFallback()) {
                tryRevertToPreviousResolvableStep();
            } else {
                startNodePolling(step, activeWindow);
            }
            return;
        }

        if (step.highlight().isPresent()) {
            highlightManager.applyHighlight(scene, step.highlight(), targetNode);
        }

        SingleWindowOverlay overlay = getOrCreateOverlay(activeWindow);
        overlay.displayStep(step, targetNode.get(), walkthrough);
    }

    public void detachAll() {
        stopNodePolling();
        highlightManager.detachAll();
        overlays.values().forEach(SingleWindowOverlay::detach);
        overlays.clear();
    }

    private void tryRevertToPreviousResolvableStep() {
        LOGGER.info("Attempting to revert to previous resolvable step");

        int currentIndex = walkthrough.currentStepProperty().get();
        for (int i = currentIndex - 1; i >= 0; i--) {
            WalkthroughNode previousStep = getStepAtIndex(i);
            Window activeWindow = previousStep.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(mainStage);
            Scene scene = activeWindow.getScene();
            if (scene != null && previousStep.resolver().resolve(scene).isPresent()) {
                LOGGER.info("Reverting to step {} from step {}", i, currentIndex);
                walkthrough.goToStep(i);
                return;
            }
        }

        LOGGER.warn("No previous resolvable step found, staying at current step");
    }

    private void startNodePolling(WalkthroughNode step, Window activeWindow) {
        LOGGER.info("Auto-fallback disabled for step: {}, starting node polling", step.title());
        stopNodePolling();

        nodePollingTimeline = new Timeline(new KeyFrame(Duration.millis(100), _ -> {
            Scene scene = activeWindow.getScene();
            if (scene != null) {
                Optional<Node> targetNode = step.resolver().resolve(scene);
                if (targetNode.isPresent()) {
                    LOGGER.info("Target node found for step: {}, displaying step", step.title());
                    stopNodePolling();

                    if (step.highlight().isPresent()) {
                        highlightManager.applyHighlight(scene, step.highlight(), targetNode);
                    }

                    SingleWindowOverlay overlay = getOrCreateOverlay(activeWindow);
                    overlay.displayStep(step, targetNode.get(), walkthrough);
                }
            }
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

    private @NonNull WalkthroughNode getStepAtIndex(int index) {
        return walkthrough.getSteps().get(index);
    }

    private SingleWindowOverlay getOrCreateOverlay(Window window) {
        return overlays.computeIfAbsent(window, SingleWindowOverlay::new);
    }
}
