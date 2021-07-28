package org.jabref.gui.externalfiles;

import javax.inject.Inject;
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
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.CheckTreeView;

public class UnlinkedFilesDialogView extends BaseDialog<Void> {

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

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private UndoManager undoManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private FileUpdateMonitor fileUpdateMonitor;

    private final ControlsFxVisualizer validationVisualizer;
    private UnlinkedFilesDialogViewModel viewModel;

    public UnlinkedFilesDialogView() {
        this.validationVisualizer = new ControlsFxVisualizer();

        this.setTitle(Localization.lang("Search for unlinked local files"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.CANCEL) {
                viewModel.cancelTasks();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new UnlinkedFilesDialogViewModel(dialogService, ExternalFileTypes.getInstance(), undoManager, fileUpdateMonitor, preferencesService, stateManager, taskExecutor);

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

        new ViewModelListCellFactory<FileExtensionViewModel>()
                .withText(FileExtensionViewModel::getDescription)
                .withIcon(FileExtensionViewModel::getIcon)
                .install(fileTypeCombo);
        fileTypeCombo.setItems(viewModel.getFileFilters());
        fileTypeCombo.valueProperty().bindBidirectional(viewModel.selectedExtensionProperty());
        fileTypeCombo.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<DateRange>()
            .withText(DateRange::getDateRange)
            .install(fileDateCombo);
        fileDateCombo.setItems(viewModel.getDateFilters());
        fileDateCombo.valueProperty().bindBidirectional(viewModel.selectedDateProperty());
        fileDateCombo.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<ExternalFileSorter>()
                .withText(ExternalFileSorter::getSorter)
                .install(fileSortCombo);
        fileSortCombo.setItems(viewModel.getSorters());
        fileSortCombo.valueProperty().bindBidirectional(viewModel.selectedSortProperty());
        fileSortCombo.getSelectionModel().selectFirst();
    }

    private void initUnlinkedFilesList() {
        new ViewModelTreeCellFactory<FileNodeViewModel>()
                .withText(FileNodeViewModel::getDisplayTextWithEditDate)
                .install(unlinkedFilesList);

        unlinkedFilesList.maxHeightProperty().bind(((Control) filePane.contentProperty().get()).heightProperty());
        unlinkedFilesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        unlinkedFilesList.rootProperty().bind(EasyBind.map(viewModel.treeRootProperty(),
                fileNode -> fileNode.map(fileNodeViewModel -> new RecursiveTreeItem<>(fileNodeViewModel, FileNodeViewModel::getChildren))
                                    .orElse(null)));

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
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>()
                .withText(item -> item).withTooltip(item -> item)
                .install(colFile);

        colMessage.setCellValueFactory(cellData -> cellData.getValue().message());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>()
                .withText(item -> item).withTooltip(item -> item)
                .install(colMessage);

        colStatus.setCellValueFactory(cellData -> cellData.getValue().icon());
        colStatus.setCellFactory(new ValueTableCellFactory<ImportFilesResultItemViewModel, JabRefIcon>().withGraphic(JabRefIcon::getGraphicNode));
        importResultTable.setColumnResizePolicy((param) -> true);

        importResultTable.setItems(viewModel.resultTableItems());
    }

    private void initButtons() {
        BooleanBinding noItemsChecked = Bindings.isNull(unlinkedFilesList.rootProperty())
                                                .or(Bindings.isEmpty(viewModel.checkedFileListProperty()));
        exportButton.disableProperty().bind(noItemsChecked);
        importButton.disableProperty().bind(noItemsChecked);

        scanButton.setDefaultButton(true);
        scanButton.disableProperty().bind(viewModel.taskActiveProperty().or(viewModel.directoryPathValidationStatus().validProperty().not()));
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
    }

    @FXML
    void exportSelected() {
        viewModel.startExport();
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
        ActionFactory factory = new ActionFactory(preferencesService.getKeyBindingRepository());

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
                case SELECT_ALL -> unlinkedFilesList.getCheckModel().checkAll();
                case UNSELECT_ALL -> unlinkedFilesList.getCheckModel().clearChecks();
                case EXPAND_ALL -> expandTree(unlinkedFilesList.getRoot(), true);
                case COLLAPSE_ALL -> expandTree(unlinkedFilesList.getRoot(), false);
            }
        }
    }
}
