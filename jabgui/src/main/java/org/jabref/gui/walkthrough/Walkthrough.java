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

import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;

/**
 * Maintains the state of a walkthrough.
 */
public class Walkthrough {
    private final IntegerProperty currentStep;
    private final IntegerProperty totalSteps;
    private final BooleanProperty active;

    private final List<WalkthroughNode> steps;
    private Optional<MultiWindowWalkthroughOverlay> overlayManager = Optional.empty();
    private Stage currentStage;

    /**
     * Creates a new walkthrough with the specified preferences.
     */
    public Walkthrough(List<WalkthroughNode> steps) {
        this.currentStep = new SimpleIntegerProperty(0);
        this.active = new SimpleBooleanProperty(false);
        this.steps = steps;
        this.totalSteps = new SimpleIntegerProperty(steps.size());
    }

    public Walkthrough(WalkthroughNode... steps) {
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
     * Gets the total number of steps property.
     *
     * @return The total steps property
     */
    public ReadOnlyIntegerProperty totalStepsProperty() {
        return totalSteps;
    }

    /**
     * Starts the walkthrough from the first step.
     *
     * @param stage The stage to display the walkthrough on
     */
    public void start(Stage stage) {
        if (currentStage != stage) {
            overlayManager.ifPresent(MultiWindowWalkthroughOverlay::detachAll);
            currentStage = stage;
            overlayManager = Optional.of(new MultiWindowWalkthroughOverlay(stage, this));
        }

        currentStep.set(0);
        active.set(true);
        getCurrentStep().ifPresent((step) -> overlayManager.ifPresent(manager -> manager.displayStep(step)));
    }

    /**
     * Moves to the next step in the walkthrough.
     */
    public void nextStep() {
        int nextIndex = currentStep.get() + 1;
        if (nextIndex < steps.size()) {
            currentStep.set(nextIndex);
            getCurrentStep().ifPresent((step) -> overlayManager.ifPresent(manager -> manager.displayStep(step)));
        } else {
            stop();
        }
    }

    /**
     * Moves to the previous step in the walkthrough.
     */
    public void previousStep() {
        int prevIndex = currentStep.get() - 1;
        if (prevIndex >= 0) {
            currentStep.set(prevIndex);
            getCurrentStep().ifPresent((step) -> overlayManager.ifPresent(manager -> manager.displayStep(step)));
        }
    }

    /**
     * Get scene of the current stage.
     */
    public Optional<Scene> getScene() {
        return Optional.ofNullable(currentStage).map(Stage::getScene);
    }

    private void stop() {
        overlayManager.ifPresent(MultiWindowWalkthroughOverlay::detachAll);
        active.set(false);
    }

    public void goToStep(int stepIndex) {
        if (stepIndex >= 0 && stepIndex < steps.size()) {
            currentStep.set(stepIndex);
            getCurrentStep().ifPresent((step) -> overlayManager.ifPresent(manager -> manager.displayStep(step)));
        }
    }

    public List<WalkthroughNode> getSteps() {
        return steps;
    }

    public void skip() {
        stop();
    }

    private Optional<WalkthroughNode> getCurrentStep() {
        int index = currentStep.get();
        if (index >= 0 && index < steps.size()) {
            return Optional.of(steps.get(index));
        }
        return Optional.empty();
    }
}
