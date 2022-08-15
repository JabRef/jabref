package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.MasterDetailPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalChangesResolverDialog extends BaseDialog<Boolean> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalChangesResolverDialog.class);

    @FXML
    public TableView<DatabaseChangeViewModel> changesTableView;
    @FXML
    public Button openAdvancedMergeDialogButton;
    @FXML
    public MasterDetailPane materDetailPane;
    @FXML
    public BorderPane changeInfoPane;
    @FXML
    private TableColumn<DatabaseChangeViewModel, String> changeName;

    private final BibDatabaseContext database;

    private final List<DatabaseChangeViewModel> changes;

    private ExternalChangesResolverViewModel viewModel;

    public ExternalChangesResolverDialog(BibDatabaseContext database, List<DatabaseChangeViewModel> changes) {
        this.database = database;
        this.changes = new ArrayList<>(changes);

        this.setTitle(Localization.lang("External changes"));
        this.getDialogPane().setPrefSize(800, 600);

        ViewLoader.view(this)
                .load().setAsDialogPane(this);

        setResultConverter(button -> {
            if (viewModel.areAllChangesResolved()) {
                LOGGER.info("External changes are resolved successfully");
                return true;
            } else {
                LOGGER.info("External changes ARE NOT resolved");
                return false;
            }
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ExternalChangesResolverViewModel(changes, database);

        changeName.setCellValueFactory(data -> data.getValue().nameProperty());

        Bindings.bindContent(changesTableView.getItems(), viewModel.getChanges());

        changesTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        changesTableView.getSelectionModel().selectFirst();

        viewModel.selectedChangeProperty().bind(changesTableView.getSelectionModel().selectedItemProperty());

        EasyBind.subscribe(viewModel.selectedChangeProperty(), selectedChange -> {
            if (selectedChange != null) {
                changeInfoPane.setCenter(selectedChange.description());
            }
        });

        openAdvancedMergeDialogButton.disableProperty().bind(viewModel.canOpenAdvancedMergeDialogProperty().not());

        EasyBind.subscribe(viewModel.areAllChangesResolvedProperty(), isResolved -> {
            if (isResolved) {
                LOGGER.info("Closing ExternalChangesResolverDialog");
                close();
            }
        });
    }

    @FXML
    public void denyChanges() {
        viewModel.denyChange();
    }

    @FXML
    public void acceptChanges() {
        viewModel.acceptChange();
    }

    @FXML
    public void openAdvancedMergeDialog() {
        viewModel.getSelectedChange().flatMap(DatabaseChangeViewModel::openAdvancedMergeDialog)
                 .ifPresent(viewModel::acceptMergedChange);
    }
}
