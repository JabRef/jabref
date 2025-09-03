package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.SideEffect;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Maintains the state of a walkthrough.
public class Walkthrough {
    private static final Logger LOGGER = LoggerFactory.getLogger(Walkthrough.class);

    private final IntegerProperty currentStep;
    private final BooleanProperty active;

    private final List<WalkthroughStep> steps;
    private final StateManager stateManager;

    private @Nullable WalkthroughOverlay overlay;
    private Stage currentStage;

    public Walkthrough(StateManager stateManager, List<WalkthroughStep> steps) {
        if (steps.isEmpty() || steps.stream().anyMatch(Objects::isNull)) {
            // This throwing is acceptable, since the Walkthrough is often hardcoded and won't make the application crash
            throw new IllegalArgumentException("Walkthrough must have at least one step and no null steps allowed.");
        }
        this.currentStep = new SimpleIntegerProperty(0);
        this.active = new SimpleBooleanProperty(false);
        this.steps = steps;
        this.stateManager = stateManager;
    }

    public Walkthrough(StateManager stateManager, @NonNull WalkthroughStep... steps) {
        this(stateManager, List.of(steps));
    }

    /// Gets the current step index property.
    ///
    /// @return The current step index property.
    public ReadOnlyIntegerProperty currentStepProperty() {
        return currentStep;
    }

    /// Starts the walkthrough from the first step.
    ///
    /// @param stage The stage to display the walkthrough on
    public void start(Stage stage) {
        if (currentStage != stage) {
            if (overlay != null) {
                overlay.detachAll();
                overlay = null;
            }
            currentStage = stage;
            overlay = new WalkthroughOverlay(stage, this);
        }

        currentStep.set(0);
        active.set(true);
        stateManager.setActiveWalkthrough(this);

        if (overlay == null) {
            LOGGER.warn("Overlay is null after initialization, cannot display step");
            return;
        }

        WalkthroughStep step = getCurrentStep();
        overlay.show(step);
    }

    /// Moves to the next step in the walkthrough.
    public void nextStep() {
        int nextIndex = currentStep.get() + 1;
        LOGGER.debug("Next step: {}", nextIndex);
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
        overlay.show(step);
    }

    /// Moves to the previous step in the walkthrough.
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
        overlay.show(step);
    }

    private void stop() {
        if (overlay != null) {
            overlay.detachAll();
        }
        active.set(false);
        stateManager.setActiveWalkthrough(null);
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
        overlay.show(step);
    }

    public @NonNull WalkthroughStep getStepAtIndex(int index) {
        return steps.get(index);
    }

    public void skip() {
        stop();
    }

    public void quit() {
        stop();
    }

    public void showQuitConfirmationAndQuit() {
        if (overlay != null) {
            overlay.showQuitConfirmationAndQuit();
        }
    }

    public @NonNull WalkthroughStep getCurrentStep() {
        return steps.get(currentStep.get());
    }

    public static Builder create(StateManager stateManager) {
        return new Builder(stateManager);
    }

    public static class Builder {
        private final StateManager stateManager;
        private final List<WalkthroughStep> steps;

        private Builder(StateManager stateManager) {
            this.stateManager = stateManager;
            this.steps = new ArrayList<>();
        }

        public Builder addStep(@NonNull WalkthroughStep step) {
            steps.add(step);
            return this;
        }

        public Builder addStep(TooltipStep.@NonNull Builder step) {
            steps.add(step.build());
            return this;
        }

        public Builder addStep(PanelStep.@NonNull Builder step) {
            steps.add(step.build());
            return this;
        }

        public Builder addStep(SideEffect.@NonNull Builder step) {
            steps.add(step.build());
            return this;
        }

        public Walkthrough build() {
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("Walkthrough must have at least one step.");
            }
            return new Walkthrough(stateManager, steps);
        }
    }
}

