package org.jabref.gui;

import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.bibtexextractor.ExtractBibtexAction;
import org.jabref.gui.citationkeypattern.GenerateCitationKeyAction;
import org.jabref.gui.cleanup.CleanupAction;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.exporter.SaveAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.GenerateEntryFromIdDialog;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.push.PushToApplicationCommand;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.UndoRedoAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.TaskProgressView;

public class MainToolBar extends ToolBar {
    private final LibraryTabContainer frame;
    private final PushToApplicationCommand pushToApplicationCommand;
    private final GlobalSearchBar globalSearchBar;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;
    private final BibEntryTypesManager entryTypesManager;
    private final CountingUndoManager undoManager;

    private PopOver entryFromIdPopOver;
    private PopOver progressViewPopOver;

    public MainToolBar(LibraryTabContainer tabContainer,
                       PushToApplicationCommand pushToApplicationCommand,
                       GlobalSearchBar globalSearchBar,
                       DialogService dialogService,
                       StateManager stateManager,
                       PreferencesService preferencesService,
                       FileUpdateMonitor fileUpdateMonitor,
                       TaskExecutor taskExecutor,
                       BibEntryTypesManager entryTypesManager,
                       CountingUndoManager undoManager) {
        this.frame = tabContainer;
        this.pushToApplicationCommand = pushToApplicationCommand;
        this.globalSearchBar = globalSearchBar;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.entryTypesManager = entryTypesManager;
        this.undoManager = undoManager;

        createToolBar();
    }

    private void createToolBar() {
        final ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        final Region leftSpacer = new Region();
        final Region rightSpacer = new Region();

        final Button pushToApplicationButton = factory.createIconButton(pushToApplicationCommand.getAction(), pushToApplicationCommand);
        pushToApplicationCommand.registerReconfigurable(pushToApplicationButton);

        // Setup Toolbar

        getItems().addAll(
                new HBox(
                        factory.createIconButton(StandardActions.NEW_LIBRARY, new NewDatabaseAction(frame, preferencesService)),
                        factory.createIconButton(StandardActions.OPEN_LIBRARY, new OpenDatabaseAction(frame, preferencesService, dialogService, stateManager, fileUpdateMonitor, entryTypesManager, undoManager, taskExecutor)),
                        factory.createIconButton(StandardActions.SAVE_LIBRARY, new SaveAction(SaveAction.SaveMethod.SAVE, frame::getCurrentLibraryTab, dialogService, preferencesService, stateManager))),

                leftSpacer,

                globalSearchBar,

                rightSpacer,

                new HBox(
                        factory.createIconButton(StandardActions.NEW_ARTICLE, new NewEntryAction(frame::getCurrentLibraryTab, StandardEntryType.Article, dialogService, preferencesService, stateManager)),
                        factory.createIconButton(StandardActions.NEW_ENTRY, new NewEntryAction(frame::getCurrentLibraryTab, dialogService, preferencesService, stateManager)),
                        createNewEntryFromIdButton(),
                        factory.createIconButton(StandardActions.NEW_ENTRY_FROM_PLAIN_TEXT, new ExtractBibtexAction(dialogService, preferencesService, stateManager)),
                        factory.createIconButton(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, frame::getCurrentLibraryTab, stateManager, undoManager))),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        factory.createIconButton(StandardActions.UNDO, new UndoRedoAction(StandardActions.UNDO, frame::getCurrentLibraryTab, dialogService, stateManager)),
                        factory.createIconButton(StandardActions.REDO, new UndoRedoAction(StandardActions.REDO, frame::getCurrentLibraryTab, dialogService, stateManager)),
                        factory.createIconButton(StandardActions.CUT, new EditAction(StandardActions.CUT, frame::getCurrentLibraryTab, stateManager, undoManager)),
                        factory.createIconButton(StandardActions.COPY, new EditAction(StandardActions.COPY, frame::getCurrentLibraryTab, stateManager, undoManager)),
                        factory.createIconButton(StandardActions.PASTE, new EditAction(StandardActions.PASTE, frame::getCurrentLibraryTab, stateManager, undoManager))),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        pushToApplicationButton,
                        factory.createIconButton(StandardActions.GENERATE_CITE_KEYS, new GenerateCitationKeyAction(frame::getCurrentLibraryTab, dialogService, stateManager, taskExecutor, preferencesService, undoManager)),
                        factory.createIconButton(StandardActions.CLEANUP_ENTRIES, new CleanupAction(frame::getCurrentLibraryTab, preferencesService, dialogService, stateManager, taskExecutor, undoManager))),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        createTaskIndicator()),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        factory.createIconButton(StandardActions.OPEN_GITHUB, new OpenBrowserAction("https://github.com/JabRef/jabref", dialogService, preferencesService.getFilePreferences()))));

        leftSpacer.setPrefWidth(50);
        leftSpacer.setMinWidth(Region.USE_PREF_SIZE);
        leftSpacer.setMaxWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(globalSearchBar, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.SOMETIMES);

        getStyleClass().add("mainToolbar");
    }

    Button createNewEntryFromIdButton() {
        Button newEntryFromIdButton = new Button();

        newEntryFromIdButton.setGraphic(IconTheme.JabRefIcons.IMPORT.getGraphicNode());
        newEntryFromIdButton.getStyleClass().setAll("icon-button");
        newEntryFromIdButton.setFocusTraversable(false);
        newEntryFromIdButton.disableProperty().bind(ActionHelper.needsDatabase(stateManager).not());
        newEntryFromIdButton.setOnMouseClicked(event -> {
            GenerateEntryFromIdDialog entryFromId = new GenerateEntryFromIdDialog(frame.getCurrentLibraryTab(), dialogService, preferencesService, taskExecutor, stateManager);

            if (entryFromIdPopOver == null) {
                entryFromIdPopOver = new PopOver(entryFromId.getDialogPane());
                entryFromIdPopOver.setTitle(Localization.lang("Import by ID"));
                entryFromIdPopOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
                entryFromIdPopOver.setContentNode(entryFromId.getDialogPane());
                entryFromIdPopOver.show(newEntryFromIdButton);
                entryFromId.setEntryFromIdPopOver(entryFromIdPopOver);
            } else if (entryFromIdPopOver.isShowing()) {
                entryFromIdPopOver.hide();
            } else {
                entryFromIdPopOver.setContentNode(entryFromId.getDialogPane());
                entryFromIdPopOver.show(newEntryFromIdButton);
                entryFromId.setEntryFromIdPopOver(entryFromIdPopOver);
            }
        });
        newEntryFromIdButton.setTooltip(new Tooltip(Localization.lang("Import by ID")));

        return newEntryFromIdButton;
    }

    Group createTaskIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.getStyleClass().add("progress-indicatorToolbar");
        indicator.progressProperty().bind(stateManager.getTasksProgress());

        Tooltip someTasksRunning = new Tooltip(Localization.lang("Background Tasks are running"));
        Tooltip noTasksRunning = new Tooltip(Localization.lang("Background Tasks are done"));
        indicator.setTooltip(noTasksRunning);
        stateManager.getAnyTaskRunning().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                indicator.setTooltip(someTasksRunning);
            } else {
                indicator.setTooltip(noTasksRunning);
            }
        });

        /*
        The label of the indicator cannot be removed with styling. Therefore,
        hide it and clip it to a square of (width x width) each time width is updated.
         */
        indicator.widthProperty().addListener((observable, oldValue, newValue) -> {
            /*
            The indeterminate spinner is wider than the determinate spinner.
            We must make sure they are the same width for the clipping to result in a square of the same size always.
             */
            if (!indicator.isIndeterminate()) {
                indicator.setPrefWidth(newValue.doubleValue());
            }
            if (newValue.doubleValue() > 0) {
                Rectangle clip = new Rectangle(newValue.doubleValue(), newValue.doubleValue());
                indicator.setClip(clip);
            }
        });

        indicator.setOnMouseClicked(event -> {
            TaskProgressView<Task<?>> taskProgressView = new TaskProgressView<Task<?>>();
            EasyBind.bindContent(taskProgressView.getTasks(), stateManager.getBackgroundTasks());
            taskProgressView.setRetainTasks(true);
            taskProgressView.setGraphicFactory(BackgroundTask::getIcon);

            if (progressViewPopOver == null) {
                progressViewPopOver = new PopOver(taskProgressView);
                progressViewPopOver.setTitle(Localization.lang("Background Tasks"));
                progressViewPopOver.setArrowLocation(PopOver.ArrowLocation.RIGHT_TOP);
                progressViewPopOver.setContentNode(taskProgressView);
                progressViewPopOver.show(indicator);
            } else if (progressViewPopOver.isShowing()) {
                progressViewPopOver.hide();
            } else {
                progressViewPopOver.setContentNode(taskProgressView);
                progressViewPopOver.show(indicator);
            }
        });

        return new Group(indicator);
    }
}
