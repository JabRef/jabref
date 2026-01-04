package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Objects;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;
import org.controlsfx.control.CheckTreeView;

public class UnlinkedFilesDialogView extends BaseDialog<Void> {
    private static final String REFRESH_CLASS = "refresh";
    private final ControlsFxVisualizer validationVisualizer;
    @FXML private TextField directoryPathField;
    @FXML private ComboBox<FileExtensionViewModel> fileTypeCombo;
    @FXML private ComboBox<DateRange> fileDateCombo;
    @FXML private ComboBox<ExternalFileSorter> fileSortCombo;
    @FXML private CheckTreeView<FileNodeViewModel> unlinkedFilesList;
    @FXML private Button scanButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    @FXML private Label progressText;
    @FXML private Accordion accordion;
    @FXML private ProgressIndicator progressDisplay;
    @FXML private VBox progressPane;
    @FXML private TableView<ImportFilesResultItemViewModel> importResultTable;
    @FXML private TableColumn<ImportFilesResultItemViewModel, JabRefIcon> colStatus;
    @FXML private TableColumn<ImportFilesResultItemViewModel, String> colMessage;
    @FXML private TableColumn<ImportFilesResultItemViewModel, String> colFile;
    @FXML private TitledPane filePane;
    @FXML private TitledPane resultPane;
    @Inject private GuiPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private UndoManager undoManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private FileUpdateMonitor fileUpdateMonitor;
    private UnlinkedFilesDialogViewModel viewModel;

    private BibDatabaseContext bibDatabaseContext;

    public UnlinkedFilesDialogView() {
        this.validationVisualizer = new ControlsFxVisualizer();

        this.setTitle(Localization.lang("Search for unlinked local files"));

        ViewLoader.view(this).load().setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.CANCEL) {
                viewModel.cancelTasks();
            }
            saveConfiguration();
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new UnlinkedFilesDialogViewModel(dialogService, undoManager, fileUpdateMonitor, preferences, stateManager, taskExecutor);

        this.bibDatabaseContext = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("No active library"));

        progressDisplay.progressProperty().bind(viewModel.progressValueProperty());
        progressText.textProperty().bind(viewModel.progressTextProperty());
        progressPane.managedProperty().bind(viewModel.taskActiveProperty());
        progressPane.visibleProperty().bind(viewModel.taskActiveProperty());
        accordion.disableProperty().bind(viewModel.taskActiveProperty());

        viewModel.treeRootProperty().addListener(observable -> {
            scanButton.setDefaultButton(false);
            importButton.setDefaultButton(true);
            scanButton.setDefaultButton(false);
            filePane.setExpanded(true);
            resultPane.setExpanded(false);
        });

        viewModel.resultTableItems().addListener((InvalidationListener) observable -> {
            filePane.setExpanded(false);
            resultPane.setExpanded(true);
            resultPane.setDisable(false);
        });

        initDirectorySelection();
        initUnlinkedFilesList();
        initResultTable();
        initButtons();
    }

    private void initDirectorySelection() {
        validationVisualizer.setDecoration(new IconValidationDecorator());

        directoryPathField.textProperty().bindBidirectional(viewModel.directoryPathProperty());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.directoryPathValidationStatus(), directoryPathField));

        new ViewModelListCellFactory<FileExtensionViewModel>().withText(FileExtensionViewModel::getDescription).withIcon(FileExtensionViewModel::getIcon).install(fileTypeCombo);
        fileTypeCombo.setItems(viewModel.getFileFilters());
        fileTypeCombo.valueProperty().bindBidirectional(viewModel.selectedExtensionProperty());

        new ViewModelListCellFactory<DateRange>().withText(DateRange::getDateRange).install(fileDateCombo);
        fileDateCombo.setItems(viewModel.getDateFilters());
        fileDateCombo.valueProperty().bindBidirectional(viewModel.selectedDateProperty());

        new ViewModelListCellFactory<ExternalFileSorter>().withText(ExternalFileSorter::getSorter).install(fileSortCombo);
        fileSortCombo.setItems(viewModel.getSorters());
        fileSortCombo.valueProperty().bindBidirectional(viewModel.selectedSortProperty());

        directoryPathField.setText(bibDatabaseContext.getFirstExistingFileDir(preferences.getFilePreferences()).map(Path::toString).orElse(""));
        loadSavedConfiguration();
    }

    private void initUnlinkedFilesList() {
        new ViewModelTreeCellFactory<FileNodeViewModel>().withText(FileNodeViewModel::getDisplayTextWithEditDate).install(unlinkedFilesList);

        unlinkedFilesList.maxHeightProperty().bind(((Control) filePane.contentProperty().get()).heightProperty());
        unlinkedFilesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unlinkedFilesList.rootProperty().bind(EasyBind.map(viewModel.treeRootProperty(), fileNode -> fileNode.map(fileNodeViewModel -> new RecursiveTreeItem<>(fileNodeViewModel, FileNodeViewModel::getChildren)).orElse(null)));

        unlinkedFilesList.setContextMenu(createSearchContextMenu());

        EasyBind.subscribe(unlinkedFilesList.rootProperty(), root -> {
            if (root != null) {
                ((CheckBoxTreeItem<FileNodeViewModel>) root).setSelected(true);
                root.setExpanded(true);
                EasyBind.bindContent(viewModel.checkedFileListProperty(), unlinkedFilesList.getCheckModel().getCheckedItems());
            } else {
                EasyBind.bindContent(viewModel.checkedFileListProperty(), FXCollections.observableArrayList());
            }
        });
    }

    private void initResultTable() {
        colFile.setCellValueFactory(cellData -> cellData.getValue().file());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>().withGraphic(this::createEllipsisLabel).withTooltip(item -> item).install(colFile);

        colMessage.setCellValueFactory(cellData -> cellData.getValue().message());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>().withGraphic(this::createEllipsisLabel).withTooltip(item -> item).install(colMessage);

        colStatus.setCellValueFactory(cellData -> cellData.getValue().icon());
        colStatus.setCellFactory(new ValueTableCellFactory<ImportFilesResultItemViewModel, JabRefIcon>().withGraphic(JabRefIcon::getGraphicNode));
        colFile.setResizable(true);
        colStatus.setResizable(true);
        colMessage.setResizable(true);
        importResultTable.setItems(viewModel.resultTableItems());
        importResultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Add context menu for "Jump to Entry"
        importResultTable.setContextMenu(createResultTableContextMenu());
    }

    private ContextMenu createResultTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        // Create "Jump to Entry" menu item
        javafx.scene.control.MenuItem jumpToEntryItem = new javafx.scene.control.MenuItem(Localization.lang("Jump to entry"));

        // Enable only when an item is selected
        jumpToEntryItem.disableProperty().bind(Bindings.isNull(importResultTable.getSelectionModel().selectedItemProperty()));

        // Action when clicked
        jumpToEntryItem.setOnAction(event -> {
            ImportFilesResultItemViewModel selected = importResultTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                viewModel.jumpToLinkedEntry(selected.file().get());
                // Close the dialog after jumping
                close();
            }
        });

        contextMenu.getItems().add(jumpToEntryItem);
        return contextMenu;
    }

    private void initButtons() {
        BooleanBinding noItemsChecked = Bindings.isNull(unlinkedFilesList.rootProperty()).or(Bindings.isEmpty(viewModel.checkedFileListProperty()));
        exportButton.disableProperty().bind(noItemsChecked);
        importButton.disableProperty().bind(noItemsChecked);

        scanButton.setDefaultButton(true);
        scanButton.disableProperty().bind(viewModel.taskActiveProperty().or(viewModel.directoryPathValidationStatus().validProperty().not()));
    }

    private void loadSavedConfiguration() {
        UnlinkedFilesDialogPreferences unlinkedFilesDialogPreferences = preferences.getUnlinkedFilesDialogPreferences();

        FileExtensionViewModel selectedExtension = fileTypeCombo.getItems().stream().filter(item -> Objects.equals(item.getName(), unlinkedFilesDialogPreferences.getUnlinkedFilesSelectedExtension())).findFirst().orElseGet(() -> new FileExtensionViewModel(StandardFileType.ANY_FILE, preferences.getExternalApplicationsPreferences()));
        fileTypeCombo.getSelectionModel().select(selectedExtension);
        fileDateCombo.getSelectionModel().select(unlinkedFilesDialogPreferences.getUnlinkedFilesSelectedDateRange());
        fileSortCombo.getSelectionModel().select(unlinkedFilesDialogPreferences.getUnlinkedFilesSelectedSort());
    }

    public void saveConfiguration() {
        preferences.getUnlinkedFilesDialogPreferences().setUnlinkedFilesSelectedExtension(fileTypeCombo.getValue().getName());
        preferences.getUnlinkedFilesDialogPreferences().setUnlinkedFilesSelectedDateRange(fileDateCombo.getValue());
        preferences.getUnlinkedFilesDialogPreferences().setUnlinkedFilesSelectedSort(fileSortCombo.getValue());
    }

    @FXML
    void browseFileDirectory() {
        viewModel.browseFileDirectory();
    }

    @FXML
    void scanFiles() {
        viewModel.startSearch();
    }

    @FXML
    void startImport() {
        viewModel.startImport();

        // Already imported files should not be re-added at a second click on "Import". Therefore, all imported files are unchecked.
        unlinkedFilesList.getCheckModel().clearChecks();

        // JavaFX does not re-render everything necessary after the file import, and hence it ends up with some misalignment (see https://github.com/JabRef/jabref/issues/12713). Thus, we remove and add the CSS property to force it to re-render.
        Platform.runLater(() -> {
            accordion.getStyleClass().remove(REFRESH_CLASS);
            accordion.getStyleClass().add(REFRESH_CLASS);
        });
    }

    @FXML
    void exportSelected() {
        viewModel.startExport();
    }

    /**
     * Creates a Label with a maximum width and ellipsis for overflow.
     * Truncates text if it exceeds two-thirds of the screen width.
     */
    private Label createEllipsisLabel(String text) {
        Label label = new Label(text);
        double maxWidth = colFile.getMaxWidth();
        label.setMaxWidth(maxWidth);
        label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        return label;
    }

    /**
     * Expands or collapses the specified tree according to the <code>expand</code>-parameter.
     */
    private void expandTree(TreeItem<?> item, boolean expand) {
        if ((item != null) && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandTree(child, expand);
            }
        }
    }

    private ContextMenu createSearchContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();

        contextMenu.getItems().add(factory.createMenuItem(StandardActions.SELECT_ALL, new SearchContextAction(StandardActions.SELECT_ALL)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.UNSELECT_ALL, new SearchContextAction(StandardActions.UNSELECT_ALL)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.EXPAND_ALL, new SearchContextAction(StandardActions.EXPAND_ALL)));
        contextMenu.getItems().add(factory.createMenuItem(StandardActions.COLLAPSE_ALL, new SearchContextAction(StandardActions.COLLAPSE_ALL)));

        return contextMenu;
    }

    private class SearchContextAction extends SimpleCommand {

        private final StandardActions command;

        public SearchContextAction(StandardActions command) {
            this.command = command;

            this.executable.bind(unlinkedFilesList.rootProperty().isNotNull());
        }

        @Override
        public void execute() {
            switch (command) {
                case SELECT_ALL ->
                        unlinkedFilesList.getCheckModel().checkAll();
                case UNSELECT_ALL ->
                        unlinkedFilesList.getCheckModel().clearChecks();
                case EXPAND_ALL ->
                        expandTree(unlinkedFilesList.getRoot(), true);
                case COLLAPSE_ALL ->
                        expandTree(unlinkedFilesList.getRoot(), false);
            }
        }
    }
}
