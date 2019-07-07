package org.jabref.gui.texparser;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.CheckTreeView;
import org.fxmisc.easybind.EasyBind;

public class ParseTexDialogView extends BaseDialog<Void> {

    private final BibDatabaseContext databaseContext;
    @FXML private TextField texDirectoryField;
    @FXML private Button browseButton;
    @FXML private Button searchButton;
    @FXML private CheckTreeView<FileNodeViewModel> fileTreeView;
    @FXML private Button selectAllButton;
    @FXML private Button unselectAllButton;
    @FXML private ButtonType parseButtonType;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;
    private ParseTexDialogViewModel viewModel;
    private ControlsFxVisualizer validationVisualizer;

    public ParseTexDialogView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.validationVisualizer = new ControlsFxVisualizer();

        this.setTitle(Localization.lang("LaTeX references search"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        Button parseButton = (Button) this.getDialogPane().lookupButton(parseButtonType);
        parseButton.disableProperty().bindBidirectional(viewModel.noFilesFoundProperty());

        setResultConverter(button -> {
            if (button == parseButtonType) {
                viewModel.parseButtonClicked();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ParseTexDialogViewModel(databaseContext, dialogService, taskExecutor, preferencesService);

        fileTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileTreeView.showRootProperty().bindBidirectional(viewModel.successfulSearchProperty());
        fileTreeView.rootProperty().bind(EasyBind.map(viewModel.rootProperty(), fileNode ->
                new RecursiveTreeItem<>(fileNode, FileNodeViewModel::getChildren)));

        EasyBind.subscribe(fileTreeView.rootProperty(), root -> {
            if (root != null) {
                ((CheckBoxTreeItem<FileNodeViewModel>) root).setSelected(true);
                root.setExpanded(true);
                EasyBind.listBind(viewModel.getCheckedFileList(), fileTreeView.getCheckModel().getCheckedItems());
            }
        });

        new ViewModelTreeCellFactory<FileNodeViewModel>()
                .withText(FileNodeViewModel::getDisplayText)
                .install(fileTreeView);

        texDirectoryField.textProperty().bindBidirectional(viewModel.texDirectoryProperty());
        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.texDirectoryValidation(), texDirectoryField));

        browseButton.disableProperty().bindBidirectional(viewModel.searchInProgressProperty());
        searchButton.disableProperty().bindBidirectional(viewModel.searchInProgressProperty());
        selectAllButton.disableProperty().bindBidirectional(viewModel.noFilesFoundProperty());
        unselectAllButton.disableProperty().bindBidirectional(viewModel.noFilesFoundProperty());
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
