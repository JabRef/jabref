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
import org.jabref.gui.walkthrough.components.PaperDirectoryChooser;
import org.jabref.gui.walkthrough.declarative.NodeResolverFactory;
import org.jabref.gui.walkthrough.declarative.WalkthroughActionsConfig;
import org.jabref.gui.walkthrough.declarative.richtext.ArbitraryJFXBlock;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;
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
    // TODO: Consider using Graph instead for complex walkthrough routing e.g., pro user show no walkthrough, new user show full walkthrough, etc.
    private final List<WalkthroughNode> steps;
    private Optional<WalkthroughOverlay> overlay = Optional.empty();
    private Stage currentStage;

    /**
     * Creates a new walkthrough with the specified preferences.
     *
     * @param preferences The walkthrough preferences to use
     */
    public Walkthrough(WalkthroughPreferences preferences) {
        this.preferences = preferences;
        this.currentStep = new SimpleIntegerProperty(0);
        this.active = new SimpleBooleanProperty(false);

        this.steps = List.of(
                WalkthroughNode.fullScreen(Localization.lang("Welcome to JabRef!"))
                               .content(
                                       new TextBlock(Localization.lang("This quick walkthrough will introduce you to some key features.")),
                                       new InfoBlock(Localization.lang("You can always access this walkthrough from the Help menu."))
                               )
                               .actions(WalkthroughActionsConfig.builder()
                                                                .continueButton(Localization.lang("Start walkthrough"))
                                                                .skipButton(Localization.lang("Skip to finish"))
                                                                .build())
                               .build(),

                WalkthroughNode.fullScreen(Localization.lang("Configure Paper Directory"))
                               .content(
                                       new TextBlock(Localization.lang("Set up your main file directory where JabRef will look for and store your PDF files and other associated documents.")),
                                       new InfoBlock(Localization.lang("This directory helps JabRef organize your paper files. You can change this later in Preferences.")),
                                       new ArbitraryJFXBlock(_ -> new PaperDirectoryChooser())
                               )
                               .actions(WalkthroughActionsConfig.all(
                                       Localization.lang("Continue"),
                                       Localization.lang("Skip for Now"),
                                       Localization.lang("Back")))
                               .build(),

                WalkthroughNode.panel(Localization.lang("Creating a New Entry"))
                               .content(
                                       new TextBlock(Localization.lang("Click the highlighted button to start creating a new bibliographic entry.")),
                                       new InfoBlock(Localization.lang("JabRef supports various entry types like articles, books, and more."))
                               )
                               .resolver(NodeResolverFactory.forAction(StandardActions.CREATE_ENTRY))
                               .position(javafx.geometry.Pos.BOTTOM_CENTER)
                               .actions(WalkthroughActionsConfig.all(
                                       Localization.lang("Continue"),
                                       Localization.lang("Skip for Now"),
                                       Localization.lang("Back")))
                               .build(),

                WalkthroughNode.panel(Localization.lang("Saving Your Work"))
                               .content(
                                       new TextBlock(Localization.lang("Don't forget to save your library. Click the save button.")),
                                       new InfoBlock(Localization.lang("Regularly saving prevents data loss."))
                               )
                               .resolver(NodeResolverFactory.forAction(StandardActions.SAVE_LIBRARY))
                               .position(javafx.geometry.Pos.CENTER_RIGHT)
                               .actions(WalkthroughActionsConfig.all(
                                       Localization.lang("Continue"),
                                       Localization.lang("Skip for Now"),
                                       Localization.lang("Back")))
                               .build(),

                WalkthroughNode.fullScreen(Localization.lang("Walkthrough Complete!"))
                               .content(
                                       new TextBlock(Localization.lang("You've completed the basic feature tour.")),
                                       new TextBlock(Localization.lang("Explore more features like groups, fetchers, and customization options.")),
                                       new InfoBlock(Localization.lang("Check our documentation for detailed guides."))
                               )
                               .actions(WalkthroughActionsConfig.builder()
                                                                .continueButton(Localization.lang("Complete walkthrough"))
                                                                .backButton(Localization.lang("Back")).build())
                               .build()
        );
        this.totalSteps = new SimpleIntegerProperty(steps.size());
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

        if (currentStage != stage) {
            overlay.ifPresent(WalkthroughOverlay::detach);
            currentStage = stage;
            overlay = Optional.of(new WalkthroughOverlay(stage, this));
        }

        currentStep.set(0);
        active.set(true);
        getCurrentStep().ifPresent((step) -> overlay.ifPresent(overlay -> overlay.displayStep(step)));
    }

    /**
     * Moves to the next step in the walkthrough.
     */
    public void nextStep() {
        int nextIndex = currentStep.get() + 1;
        if (nextIndex < steps.size()) {
            currentStep.set(nextIndex);
            getCurrentStep().ifPresent((step) -> overlay.ifPresent(overlay -> overlay.displayStep(step)));
        } else {
            preferences.setCompleted(true);
            stop();
        }
    }

    /**
     * Moves to the next step in the walkthrough with stage switching. This method
     * handles stage changes by recreating the overlay on the new stage.
     *
     * @param stage The stage to display the next step on
     */
    public void nextStep(Stage stage) {
        if (currentStage != stage) {
            overlay.ifPresent(WalkthroughOverlay::detach);
            currentStage = stage;
            overlay = Optional.of(new WalkthroughOverlay(stage, this));
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
            getCurrentStep().ifPresent((step) -> overlay.ifPresent(overlay -> overlay.displayStep(step)));
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
        overlay.ifPresent(WalkthroughOverlay::detach);
        active.set(false);
    }

    private Optional<WalkthroughNode> getCurrentStep() {
        int index = currentStep.get();
        if (index >= 0 && index < steps.size()) {
            return Optional.of(steps.get(index));
        }
        return Optional.empty();
    }
}
