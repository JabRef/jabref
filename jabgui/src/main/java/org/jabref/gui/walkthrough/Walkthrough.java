package org.jabref.gui.walkthrough;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state of a walkthrough.
 */
public class Walkthrough {
    private static final Logger LOGGER = LoggerFactory.getLogger(Walkthrough.class);

    private final IntegerProperty currentStep;
    private final BooleanProperty active;

    private final List<WalkthroughStep> steps;
    private @Nullable WalkthroughOverlay overlay;
    private Stage currentStage;

    /**
     * Creates a new walkthrough with steps
     */
    public Walkthrough(List<WalkthroughStep> steps) {
        this.currentStep = new SimpleIntegerProperty(0);
        this.active = new SimpleBooleanProperty(false);
        this.steps = steps;
    }

    /**
     * Creates a new walkthrough with steps
     */
    public Walkthrough(WalkthroughStep... steps) {
        this(List.of(steps));
    }

    /**
     * Gets the current step index property.
     *
     * @return The current step index property.
     */
    public ReadOnlyIntegerProperty currentStepProperty() {
        return currentStep;
    }

    /**
     * Starts the walkthrough from the first step.
     *
     * @param stage The stage to display the walkthrough on
     */
    public void start(Stage stage) {
        if (currentStage != stage) {
            if (overlay != null) {
                overlay.detachAll();
            }
            currentStage = stage;
            overlay = new WalkthroughOverlay(stage, this);
        }

        currentStep.set(0);
        active.set(true);

        if (overlay == null) {
            LOGGER.warn("Overlay is null after initialization, cannot display step");
            return;
        }

        WalkthroughStep step = getCurrentStep();
        overlay.displayStep(step);
    }

    /**
     * Moves to the next step in the walkthrough.
     */
    public void nextStep() {
        int nextIndex = currentStep.get() + 1;
        if (nextIndex >= steps.size()) {
            stop();
            return;
        }

        currentStep.set(nextIndex);
        if (overlay == null) {
            LOGGER.warn("Overlay is null, cannot display next step");
            return;
        }

        WalkthroughStep step = getCurrentStep();
        overlay.displayStep(step);
    }

    /**
     * Moves to the previous step in the walkthrough.
     */
    public void previousStep() {
        int prevIndex = currentStep.get() - 1;
        if (prevIndex < 0) {
            return;
        }

        currentStep.set(prevIndex);
        if (overlay == null) {
            LOGGER.warn("Overlay is null, cannot display previous step");
            return;
        }

        WalkthroughStep step = getCurrentStep();
        overlay.displayStep(step);
    }

    /**
     * Get scene of the current stage.
     */
    public Optional<Scene> getScene() {
        return Optional.ofNullable(currentStage).map(Stage::getScene);
    }

    private void stop() {
        if (overlay != null) {
            overlay.detachAll();
        }
        active.set(false);
    }

    public void goToStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= steps.size()) {
            LOGGER.debug("Invalid step index: {}. Valid range is 0 to {}.", stepIndex, steps.size() - 1);
            return;
        }

        currentStep.set(stepIndex);
        if (overlay == null) {
            LOGGER.warn("Overlay is null, cannot go to step {}", stepIndex);
            return;
        }

        WalkthroughStep step = getCurrentStep();
        overlay.displayStep(step);
    }

    public List<WalkthroughStep> getSteps() {
        return steps;
    }

    public void skip() {
        stop();
    }

    private WalkthroughStep getCurrentStep() {
        return steps.get(currentStep.get());
    }
}

