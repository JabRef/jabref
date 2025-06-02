package org.jabref.gui.walkthrough;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.stage.Stage;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.walkthrough.declarative.InfoBlockContentBlock;
import org.jabref.gui.walkthrough.declarative.NodeResolverFactory;
import org.jabref.gui.walkthrough.declarative.StepType;
import org.jabref.gui.walkthrough.declarative.TextContentBlock;
import org.jabref.gui.walkthrough.declarative.WalkthroughActionsConfig;
import org.jabref.gui.walkthrough.declarative.WalkthroughStep;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.WalkthroughPreferences;

/**
 * Manages a walkthrough session by coordinating steps.
 */
public class Walkthrough {
    private final WalkthroughPreferences preferences;
    private final IntegerProperty currentStep;
    private final IntegerProperty totalSteps;
    private final BooleanProperty active;
    private final WalkthroughStep[] steps;
    private WalkthroughOverlay overlay;
    private Stage currentStage;

    /**
     * Creates a new walkthrough manager with the specified preferences.
     *
     * @param preferences The walkthrough preferences to use
     */
    public Walkthrough(WalkthroughPreferences preferences) {
        this.preferences = preferences;
        this.currentStep = new SimpleIntegerProperty(0);
        this.active = new SimpleBooleanProperty(false);

        this.steps = new WalkthroughStep[] {
                new WalkthroughStep(
                        Localization.lang("Walkthrough welcome title"),
                        StepType.FULL_SCREEN,
                        List.of(
                                new TextContentBlock(Localization.lang("Walkthrough welcome intro")),
                                new InfoBlockContentBlock(Localization.lang("Walkthrough welcome tip"))),
                        new WalkthroughActionsConfig(Optional.of("Start walkthrough"),
                                Optional.of("Skip to finish"), Optional.empty())),

                new WalkthroughStep(
                        Localization.lang("Walkthrough create entry title"),
                        StepType.BOTTOM_PANEL,
                        List.of(
                                new TextContentBlock(Localization.lang("Walkthrough create entry description")),
                                new InfoBlockContentBlock(Localization.lang("Walkthrough create entry tip"))),
                        NodeResolverFactory.forAction(StandardActions.CREATE_ENTRY)
                ),

                new WalkthroughStep(
                        Localization.lang("Walkthrough save title"),
                        StepType.RIGHT_PANEL,
                        List.of(
                                new TextContentBlock(Localization.lang("Walkthrough save description")),
                                new InfoBlockContentBlock(Localization.lang("Walkthrough save important"))),
                        NodeResolverFactory.forAction(StandardActions.SAVE_LIBRARY)),

                new WalkthroughStep(
                        Localization.lang("Walkthrough completion title"),
                        StepType.FULL_SCREEN,
                        List.of(
                                new TextContentBlock(Localization.lang("Walkthrough completion message")),
                                new TextContentBlock(Localization.lang("Walkthrough completion next_steps")),
                                new InfoBlockContentBlock(Localization.lang("Walkthrough completion resources"))),
                        new WalkthroughActionsConfig(Optional.of("Complete walkthrough"), Optional.empty(),
                                Optional.of("Back")))
        };

        this.totalSteps = new SimpleIntegerProperty(steps.length);
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
     * Checks if the walkthrough is completed based on preferences.
     *
     * @return true if the walkthrough has been completed
     */
    public boolean isCompleted() {
        return preferences.isCompleted();
    }

    /**
     * Starts the walkthrough from the first step.
     *
     * @param stage The stage to display the walkthrough on
     */
    public void start(Stage stage) {
        if (preferences.isCompleted()) {
            return;
        }

        if (currentStage != stage || overlay == null) {
            if (overlay != null) {
                overlay.detach();
            }
            currentStage = stage;
            overlay = new WalkthroughOverlay(stage, this);
        }

        currentStep.set(0);
        active.set(true);
        overlay.displayStep(getCurrentStep());
    }

    /**
     * Moves to the next step in the walkthrough.
     */
    public void nextStep() {
        int nextIndex = currentStep.get() + 1;
        if (nextIndex < steps.length) {
            currentStep.set(nextIndex);
            if (overlay != null) {
                overlay.displayStep(getCurrentStep());
            }
        } else {
            preferences.setCompleted(true);
            stop();
        }
    }

    /**
     * Moves to the next step in the walkthrough with stage switching.
     * This method handles stage changes by recreating the overlay on the new stage.
     *
     * @param stage The stage to display the next step on
     */
    public void nextStep(Stage stage) {
        if (currentStage != stage) {
            if (overlay != null) {
                overlay.detach();
            }
            currentStage = stage;
            overlay = new WalkthroughOverlay(stage, this);
        }
        nextStep();
    }

    /**
     * Moves to the previous step in the walkthrough.
     */
    public void previousStep() {
        int prevIndex = currentStep.get() - 1;
        if (prevIndex >= 0) {
            currentStep.set(prevIndex);
            if (overlay != null) {
                overlay.displayStep(getCurrentStep());
            }
        }
    }

    /**
     * Skips the walkthrough completely.
     */
    public void skip() {
        preferences.setCompleted(true);
        stop();
    }

    private void stop() {
        if (overlay != null) {
            overlay.detach();
        }
        active.set(false);
    }

    private WalkthroughStep getCurrentStep() {
        int index = currentStep.get();
        if (index >= 0 && index < steps.length) {
            return steps[index];
        }
        return null;
    }
}
