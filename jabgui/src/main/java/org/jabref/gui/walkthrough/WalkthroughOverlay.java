package org.jabref.gui.walkthrough;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);

    private final Map<Window, SingleWindowWalkthroughOverlay> overlays = new HashMap<>();
    private final Stage stage;
    private final WalkthroughHighlighter walkthroughHighlighter;
    private final Walkthrough walkthrough;
    private @Nullable Timeline nodePollingTimeline;

    public WalkthroughOverlay(Stage stage, Walkthrough walkthrough) {
        this.stage = stage;
        this.walkthrough = walkthrough;
        this.walkthroughHighlighter = new WalkthroughHighlighter();
    }

    /**
     * Displays the specified walkthrough step in the appropriate window.
     */
    public void displayStep(@NonNull WalkthroughStep step) {
        overlays.values().forEach(SingleWindowWalkthroughOverlay::hide);
        Window window = step.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(stage);
        Scene scene = window.getScene();

        Optional<Node> targetNode = step.resolver().flatMap(resolver -> resolver.resolve(scene));

        if (step.resolver().isPresent()) {
            startNodePolling(step, window, targetNode.orElse(null));
        } else {
            walkthroughHighlighter.applyHighlight(step.highlight().orElse(null), scene, null);
            displayStep(step, window, null);
        }
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

    private void displayStep(WalkthroughStep step, Window window, @Nullable Node node) {
        SingleWindowWalkthroughOverlay overlay = getOrCreateOverlay(window);
        overlay.displayStep(step, node, this::stopNodePolling, walkthrough);
    }

    private void startNodePolling(WalkthroughStep step, Window window, @Nullable Node node) {
        stopNodePolling();

        AtomicBoolean nodeEverResolved = new AtomicBoolean(node != null);

        Scene initialScene = window.getScene();

        walkthroughHighlighter.applyHighlight(step.highlight().orElse(null), initialScene, node);
        displayStep(step, window, node);

        LOGGER.info("Starting continuous node polling for step: {}", step.title());

        nodePollingTimeline = new Timeline(new KeyFrame(Duration.millis(500), _ -> {
            Scene scene = window.getScene();
            if (scene == null) {
                return;
            }

            step.resolver().flatMap(resolver -> resolver.resolve(scene)).ifPresentOrElse(
                    (currentNode) -> {
                        if (!nodeEverResolved.get()) {
                            LOGGER.info("Target node found for step: {}, updating display", step.title());
                            walkthroughHighlighter.applyHighlight(step.highlight().orElse(null), scene, currentNode);
                            displayStep(step, window, currentNode);
                            nodeEverResolved.set(true);
                        }
                    },
                    () -> {
                        if (!nodeEverResolved.get()) {
                            return;
                        }

                        LOGGER.info("Node disappeared for step: {}, auto-falling back", step.title());
                        stopNodePolling();
                        tryRevertToPreviousResolvableStep();
                    }
            );
        }));

        nodePollingTimeline.setCycleCount(Timeline.INDEFINITE);
        nodePollingTimeline.play();
    }

    private void stopNodePolling() {
        LOGGER.info("Stopping node polling for step.");
        if (nodePollingTimeline == null) {
            return;
        }
        nodePollingTimeline.stop();
        nodePollingTimeline = null;
    }

    private SingleWindowWalkthroughOverlay getOrCreateOverlay(Window window) {
        return overlays.computeIfAbsent(window, SingleWindowWalkthroughOverlay::new);
    }
}
