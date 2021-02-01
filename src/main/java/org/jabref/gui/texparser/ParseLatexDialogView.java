package org.jabref.gui.texparser;

import javax.inject.Inject;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.CheckTreeView;

public class ParseLatexDialogView extends BaseDialog<Void> {

    private final BibDatabaseContext databaseContext;
    private final ControlsFxVisualizer validationVisualizer;
    @FXML private TextField latexDirectoryField;
    @FXML private Button browseButton;
    @FXML private Button searchButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private CheckTreeView<FileNodeViewModel> fileTreeView;
    @FXML private Button selectAllButton;
    @FXML private Button unselectAllButton;
    @FXML private ButtonType parseButtonType;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;
    @Inject private FileUpdateMonitor fileMonitor;
    private ParseLatexDialogViewModel viewModel;

    public ParseLatexDialogView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.validationVisualizer = new ControlsFxVisualizer();

        setTitle(Localization.lang("Search for citations in LaTeX files..."));

        ViewLoader.view(this).load().setAsDialogPane(this);

        ControlHelper.setAction(parseButtonType, getDialogPane(), event -> viewModel.parseButtonClicked());
        Button parseButton = (Button) getDialogPane().lookupButton(parseButtonType);
        parseButton.disableProperty().bind(viewModel.noFilesFoundProperty().or(
                Bindings.isEmpty(viewModel.getCheckedFileList())));
    }

    @FXML
    private void initialize() {
        viewModel = new ParseLatexDialogViewModel(databaseContext, dialogService, taskExecutor, preferencesService, fileMonitor);

        fileTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileTreeView.showRootProperty().bindBidirectional(viewModel.successfulSearchProperty());
        fileTreeView.rootProperty().bind(EasyBind.map(viewModel.rootProperty(), fileNode ->
                new RecursiveTreeItem<>(fileNode, FileNodeViewModel::getChildren)));

        new ViewModelTreeCellFactory<FileNodeViewModel>()
                .withText(FileNodeViewModel::getDisplayText)
                .install(fileTreeView);

        EasyBind.subscribe(fileTreeView.rootProperty(), root -> {
            ((CheckBoxTreeItem<FileNodeViewModel>) root).setSelected(true);
            root.setExpanded(true);
            EasyBind.bindContent(viewModel.getCheckedFileList(), fileTreeView.getCheckModel().getCheckedItems());
        });

        latexDirectoryField.textProperty().bindBidirectional(viewModel.latexFileDirectoryProperty());
        validationVisualizer.setDecoration(new IconValidationDecorator());
        validationVisualizer.initVisualization(viewModel.latexDirectoryValidation(), latexDirectoryField);
        browseButton.disableProperty().bindBidirectional(viewModel.searchInProgressProperty());
        searchButton.disableProperty().bind(viewModel.latexDirectoryValidation().validProperty().not());
        selectAllButton.disableProperty().bindBidirectional(viewModel.noFilesFoundProperty());
        unselectAllButton.disableProperty().bindBidirectional(viewModel.noFilesFoundProperty());
        progressIndicator.visibleProperty().bindBidirectional(viewModel.searchInProgressProperty());
    }

    @FXML
    private void browseButtonClicked() {
        viewModel.browseButtonClicked();
    }

    @FXML
    private void searchButtonClicked() {
        viewModel.searchButtonClicked();
    }

    @FXML
    private void selectAll() {
        fileTreeView.getCheckModel().checkAll();
    }

    @FXML
    private void unselectAll() {
        fileTreeView.getCheckModel().clearChecks();
    }
}
