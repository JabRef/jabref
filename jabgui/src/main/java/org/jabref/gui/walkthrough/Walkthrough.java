package org.jabref.gui.walkthrough;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.walkthrough.components.PaperDirectoryChooser;
import org.jabref.gui.walkthrough.declarative.NodeResolverFactory;
import org.jabref.gui.walkthrough.declarative.WalkthroughActionsConfig;
import org.jabref.gui.walkthrough.declarative.richtext.ArbitraryJFXBlock;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.step.FullScreenStep;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughNode;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.WalkthroughPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;

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
    private final BibDatabaseContext database;

    /**
     * Creates a new walkthrough with the specified preferences.
     *
     * @param preferences The walkthrough preferences to use
     */
    public Walkthrough(WalkthroughPreferences preferences, @NonNull JabRefFrame frame) {
        this.preferences = preferences;

        this.currentStep = new SimpleIntegerProperty(0);
        this.active = new SimpleBooleanProperty(false);
        this.database = new BibDatabaseContext();

        FullScreenStep welcomeNode = WalkthroughNode
                .fullScreen(Localization.lang("Welcome to JabRef"))
                .content(
                        new TextBlock(Localization.lang("This quick walkthrough will introduce you to some key features.")),
                        new InfoBlock(Localization.lang("You can always access this walkthrough from the Help menu."))
                )
                .actions(WalkthroughActionsConfig.builder()
                                                 .continueButton(Localization.lang("Start walkthrough"))
                                                 .skipButton(Localization.lang("Skip to finish"))
                                                 .build())
                .build();
        WalkthroughActionsConfig actions = WalkthroughActionsConfig.all(
                Localization.lang("Continue"),
                Localization.lang("Skip for Now"),
                Localization.lang("Back"));
        FullScreenStep paperDirectoryNode = WalkthroughNode
                .fullScreen(Localization.lang("Configure paper directory"))
                .content(
                        new TextBlock(Localization.lang("Set up your main file directory where JabRef will look for and store your PDF files and other associated documents.")),
                        new InfoBlock(Localization.lang("This directory helps JabRef organize your paper files. You can change this later in Preferences.")),
                        new ArbitraryJFXBlock(_ -> new PaperDirectoryChooser())
                )
                .actions(actions)
                .nextStepAction(walkthrough -> {
                    if (frame.getCurrentLibraryTab() != null) {
                        walkthrough.nextStep();
                        return;
                    }

                    DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
                    AiService aiService = Injector.instantiateModelOrService(AiService.class);
                    GuiPreferences guiPreferences = Injector.instantiateModelOrService(GuiPreferences.class);
                    StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);
                    FileUpdateMonitor fileUpdateMonitor = Injector.instantiateModelOrService(FileUpdateMonitor.class);
                    BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
                    CountingUndoManager undoManager = Injector.instantiateModelOrService(CountingUndoManager.class);
                    ClipBoardManager clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
                    TaskExecutor taskExecutor = Injector.instantiateModelOrService(TaskExecutor.class);

                    LibraryTab libraryTab = LibraryTab.createLibraryTab(
                            this.database,
                            frame,
                            dialogService,
                            aiService,
                            guiPreferences,
                            stateManager,
                            fileUpdateMonitor,
                            entryTypesManager,
                            undoManager,
                            clipBoardManager,
                            taskExecutor);
                    frame.addTab(libraryTab, true);
                    walkthrough.nextStep();
                })
                .build();
        PanelStep createNode = WalkthroughNode
                .panel(Localization.lang("Creating a new entry"))
                .content(
                        new TextBlock(Localization.lang("Click the highlighted button to start creating a new bibliographic entry.")),
                        new InfoBlock(Localization.lang("JabRef supports various entry types like articles, books, and more.")),
                        new TextBlock(Localization.lang("In the entry editor that opens after clicking the button, choose \"Article\" as the entry type."))
                )
                .resolver(NodeResolverFactory.forAction(StandardActions.CREATE_ENTRY))
                .position(Pos.BOTTOM_CENTER)
                .actions(actions)
                .nextStepAction(walkthrough -> {
                    NodeResolverFactory.forAction(StandardActions.CREATE_ENTRY).apply(walkthrough.currentStage.getScene()).ifPresent(node -> {
                        if (node instanceof Button button) {
                            button.fire();
                        }
                    });
                    walkthrough.nextStep(frame.getMainStage());
                })
                .build();
        // FIXME: Index out of bound.
        PanelStep editNode = WalkthroughNode
                .panel(Localization.lang("Fill in the entry details"))
                .content(
                        new TextBlock(Localization.lang("In the title field, enter \"JabRef: BibTeX-based literature management software\".")),
                        new TextBlock(Localization.lang("In the journal field, enter \"TUGboat\".")),
                        new InfoBlock(Localization.lang("You can fill in more details later. JabRef supports many entry types and fields."))
                )
                .resolver(NodeResolverFactory.forSelector(".editorPane"))
                .position(Pos.TOP_CENTER)
                .actions(actions)
                .nextStepAction(walkthrough -> {
                    BibEntry exampleEntry = new BibEntry(StandardEntryType.Article)
                            .withField(StandardField.AUTHOR, "Oliver Kopp and Carl Christian Snethlage and Christoph Schwentker")
                            .withField(StandardField.TITLE, "JabRef: BibTeX-based literature management software")
                            .withField(StandardField.JOURNAL, "TUGboat")
                            .withField(StandardField.VOLUME, "44")
                            .withField(StandardField.NUMBER, "3")
                            .withField(StandardField.PAGES, "441--447")
                            .withField(StandardField.DOI, "10.47397/tb/44-3/tb138kopp-jabref")
                            .withField(StandardField.ISSN, "0896-3207")
                            .withField(StandardField.ISSUE, "138")
                            .withField(StandardField.YEAR, "2023")
                            .withChanged(true);
                    var db = database.getDatabase();
                    for (BibEntry entry : db.getEntries()) {
                        db.removeEntry(entry);
                    }
                    db.insertEntry(exampleEntry);
                    walkthrough.nextStep(frame.getMainStage());
                })
                .build();
        // FIXME: Index out of bound.
        PanelStep saveNode = WalkthroughNode
                .panel(Localization.lang("Saving your work"))
                .content(
                        new TextBlock(Localization.lang("Don't forget to save your library. Click the save button.")),
                        new InfoBlock(Localization.lang("Regularly saving prevents data loss."))
                )
                .resolver(NodeResolverFactory.forAction(StandardActions.SAVE_LIBRARY))
                .position(Pos.CENTER_RIGHT)
                .actions(actions)
                .nextStepAction(walkthrough -> {
                    NodeResolverFactory.forAction(StandardActions.SAVE_LIBRARY)
                                       .apply(walkthrough.currentStage.getScene())
                                       .ifPresent(node -> {
                                           if (node instanceof Button button) {
                                               button.fire();
                                           }
                                       });
                    walkthrough.nextStep();
                })
                .build();
        FullScreenStep completeNode = WalkthroughNode
                .fullScreen(Localization.lang("Walkthrough complete"))
                .content(
                        new TextBlock(Localization.lang("You've completed the basic feature tour.")),
                        new TextBlock(Localization.lang("Explore more features like groups, fetchers, and customization options.")),
                        new InfoBlock(Localization.lang("Check our documentation for detailed guides."))
                )
                .actions(WalkthroughActionsConfig.builder()
                                                 .continueButton(Localization.lang("Complete walkthrough"))
                                                 .backButton(Localization.lang("Back")).build())
                .build();
        this.steps = List.of(welcomeNode, paperDirectoryNode, createNode, editNode, saveNode, completeNode);
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
