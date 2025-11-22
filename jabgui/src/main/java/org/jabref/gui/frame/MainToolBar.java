package org.jabref.gui.frame;

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

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.citationkeypattern.GenerateCitationKeyAction;
import org.jabref.gui.cleanup.CleanupAction;
import org.jabref.gui.edit.EditAction;
import org.jabref.gui.edit.OpenBrowserAction;
import org.jabref.gui.exporter.SaveAction;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.push.GuiPushToApplicationCommand;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.TaskProgressView;

public class MainToolBar extends ToolBar {
    private final LibraryTabContainer frame;
    private final GuiPushToApplicationCommand pushToApplicationCommand;
    private final GlobalSearchBar globalSearchBar;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final GuiPreferences preferences;
    private final AiService aiService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final DirectoryUpdateMonitor directoryUpdateMonitor;
    private final TaskExecutor taskExecutor;
    private final BibEntryTypesManager entryTypesManager;
    private final ClipBoardManager clipBoardManager;
    private SimpleCommand backCommand;
    private SimpleCommand forwardCommand;
    private final CountingUndoManager undoManager;

    private PopOver entryFromIdPopOver;
    private PopOver progressViewPopOver;
    private Subscription taskProgressSubscription;

    public MainToolBar(LibraryTabContainer tabContainer,
                       GuiPushToApplicationCommand pushToApplicationCommand,
                       GlobalSearchBar globalSearchBar,
                       DialogService dialogService,
                       StateManager stateManager,
                       GuiPreferences preferences,
                       AiService aiService,
                       FileUpdateMonitor fileUpdateMonitor,
                       DirectoryUpdateMonitor directoryUpdateMonitor,
                       TaskExecutor taskExecutor,
                       BibEntryTypesManager entryTypesManager,
                       ClipBoardManager clipBoardManager,
                       CountingUndoManager undoManager) {
        this.frame = tabContainer;
        this.pushToApplicationCommand = pushToApplicationCommand;
        this.globalSearchBar = globalSearchBar;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.aiService = aiService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.directoryUpdateMonitor = directoryUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.entryTypesManager = entryTypesManager;
        this.clipBoardManager = clipBoardManager;
        this.undoManager = undoManager;

        createToolBar();
    }

    private void createToolBar() {
        final ActionFactory factory = new ActionFactory();

        final Region leftSpacer = new Region();
        final Region rightSpacer = new Region();

        final Button pushToApplicationButton = factory.createIconButton(pushToApplicationCommand.getAction(), pushToApplicationCommand);
        pushToApplicationCommand.registerReconfigurable(pushToApplicationButton);
        initNavigationCommands();

        // Setup Toolbar

        getItems().addAll(
                new HBox(
                        factory.createIconButton(StandardActions.NEW_LIBRARY, new NewDatabaseAction(frame, preferences)),
                        factory.createIconButton(StandardActions.OPEN_LIBRARY, new OpenDatabaseAction(frame, preferences, aiService, dialogService, stateManager, fileUpdateMonitor, directoryUpdateMonitor, entryTypesManager, undoManager, clipBoardManager, taskExecutor)),
                        factory.createIconButton(StandardActions.SAVE_LIBRARY, new SaveAction(SaveAction.SaveMethod.SAVE, frame::getCurrentLibraryTab, dialogService, preferences, stateManager))),

                leftSpacer,

                globalSearchBar,

                rightSpacer,

                new HBox(
                        factory.createIconButton(StandardActions.ADD_ENTRY_IMMEDIATE, new NewEntryAction(true, frame::getCurrentLibraryTab, dialogService, preferences, stateManager)),
                        factory.createIconButton(StandardActions.ADD_ENTRY, new NewEntryAction(false, frame::getCurrentLibraryTab, dialogService, preferences, stateManager)),
                        factory.createIconButton(StandardActions.DELETE_ENTRY, new EditAction(StandardActions.DELETE_ENTRY, frame::getCurrentLibraryTab, stateManager, undoManager))),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        factory.createIconButton(StandardActions.BACK, backCommand),
                        factory.createIconButton(StandardActions.FORWARD, forwardCommand)),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        factory.createIconButton(StandardActions.UNDO, new UndoAction(frame::getCurrentLibraryTab, undoManager, dialogService, stateManager)),
                        factory.createIconButton(StandardActions.REDO, new RedoAction(frame::getCurrentLibraryTab, undoManager, dialogService, stateManager)),
                        factory.createIconButton(StandardActions.CUT, new EditAction(StandardActions.CUT, frame::getCurrentLibraryTab, stateManager, undoManager)),
                        factory.createIconButton(StandardActions.COPY, new EditAction(StandardActions.COPY, frame::getCurrentLibraryTab, stateManager, undoManager)),
                        factory.createIconButton(StandardActions.PASTE, new EditAction(StandardActions.PASTE, frame::getCurrentLibraryTab, stateManager, undoManager))),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        pushToApplicationButton,
                        factory.createIconButton(StandardActions.GENERATE_CITE_KEYS, new GenerateCitationKeyAction(frame::getCurrentLibraryTab, dialogService, stateManager, taskExecutor, preferences, undoManager)),
                        factory.createIconButton(StandardActions.CLEANUP_ENTRIES, new CleanupAction(frame::getCurrentLibraryTab, preferences, dialogService, stateManager, taskExecutor, undoManager))),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        createTaskIndicator()),

                new Separator(Orientation.VERTICAL),

                new HBox(
                        factory.createIconButton(StandardActions.OPEN_GITHUB, new OpenBrowserAction("https://github.com/JabRef/jabref", dialogService, preferences.getExternalApplicationsPreferences()))));

        leftSpacer.setPrefWidth(50);
        leftSpacer.setMinWidth(Region.USE_PREF_SIZE);
        leftSpacer.setMaxWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(globalSearchBar, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.SOMETIMES);

        getStyleClass().add("mainToolbar");
    }

    Group createTaskIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.getStyleClass().add("progress-indicatorToolbar");
        indicator.progressProperty().bind(stateManager.getTasksProgress());

        Tooltip someTasksRunning = new Tooltip(Localization.lang("Background tasks are running"));
        Tooltip noTasksRunning = new Tooltip(Localization.lang("Background tasks are finished"));
        indicator.setTooltip(noTasksRunning);
        stateManager.getAnyTaskRunning().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                indicator.setTooltip(someTasksRunning);
            } else {
                indicator.setTooltip(noTasksRunning);
            }
        });

        // The label of the indicator cannot be removed with styling. Therefore,
        // hide it and clip it to a square of (width x width) each time width is updated.
        indicator.widthProperty().addListener((observable, oldValue, newValue) -> {
            // The indeterminate spinner is wider than the determinate spinner.
            // We must make sure they are the same width for the clipping to result in a square of the same size always.
            if (!indicator.isIndeterminate()) {
                indicator.setPrefWidth(newValue.doubleValue());
            }
            if (newValue.doubleValue() > 0) {
                Rectangle clip = new Rectangle(newValue.doubleValue(), newValue.doubleValue());
                indicator.setClip(clip);
            }
        });

        indicator.setOnMouseClicked(event -> {
            if ((progressViewPopOver != null) && (progressViewPopOver.isShowing())) {
                progressViewPopOver.hide();
                taskProgressSubscription.unsubscribe();
                return;
            }

            TaskProgressView<Task<?>> taskProgressView = new TaskProgressView<>();
            taskProgressSubscription = EasyBind.bindContent(taskProgressView.getTasks(), stateManager.getRunningBackgroundTasks());
            taskProgressView.setRetainTasks(false);
            taskProgressView.setGraphicFactory(task -> ThemeManager.getDownloadIconTitleMap.getOrDefault(task.getTitle(), null));

            if (progressViewPopOver == null) {
                progressViewPopOver = new PopOver(taskProgressView);
                progressViewPopOver.setTitle(Localization.lang("Background tasks"));
                progressViewPopOver.setArrowLocation(PopOver.ArrowLocation.RIGHT_TOP);
            }

            progressViewPopOver.setContentNode(taskProgressView);
            progressViewPopOver.show(indicator);
        });

        return new Group(indicator);
    }

    private void initNavigationCommands() {
        backCommand = new SimpleCommand() {
            {
                executable.bind(stateManager.canGoBackProperty());
            }

            @Override
            public void execute() {
                if (frame.getCurrentLibraryTab() != null) {
                    frame.getCurrentLibraryTab().back();
                }
            }
        };

        forwardCommand = new SimpleCommand() {
            {
                executable.bind(stateManager.canGoForwardProperty());
            }

            @Override
            public void execute() {
                if (frame.getCurrentLibraryTab() != null) {
                    frame.getCurrentLibraryTab().forward();
                }
            }
        };
    }
}
