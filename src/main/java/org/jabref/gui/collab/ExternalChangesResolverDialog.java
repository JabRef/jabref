package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalChangesResolverDialog extends BaseDialog<Boolean> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalChangesResolverDialog.class);

    @FXML
    public TableView<DatabaseChangeViewModel> changesTableView;
    @FXML
    private TableColumn<DatabaseChangeViewModel, String> changeName;

    private final BibDatabaseContext database;
    private final NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));

    private final List<DatabaseChangeViewModel> acceptedChanges = new ArrayList<>();

    public ExternalChangesResolverDialog(BibDatabaseContext database, List<DatabaseChangeViewModel> changes) {
        this.database = database;

        this.setTitle(Localization.lang("External changes"));
        this.getDialogPane().setPrefSize(800, 600);

        ViewLoader.view(this)
                .load().setAsDialogPane(this);
        changesTableView.itemsProperty().set(FXCollections.observableList(changes));
        changesTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setResultConverter(button -> {
            if (!changesTableView.getItems().isEmpty()) {
                LOGGER.info("External changes ARE NOT resolved");
                return false;
            } else {
                LOGGER.info("External changes are resolved successfully");
                applyChanges();
                // TODO: panel.getUndoManager().addEdit(ce);
                return true;
            }
        });
    }

    @FXML
    private void initialize() {
        changeName.setCellValueFactory(data -> data.getValue().nameProperty());
    }

    @FXML
    public void denyChanges() {
        changesTableView.getItems().removeAll(changesTableView.getSelectionModel().getSelectedItems());
    }

    @FXML
    public void acceptChanges() {
        // Changes will be applied when closing the dialog as a transaction
        acceptedChanges.addAll(changesTableView.getSelectionModel().getSelectedItems());
        changesTableView.getItems().removeAll(changesTableView.getSelectionModel().getSelectedItems());
    }

    @FXML
    public void openAdvancedMergeDialog() {
    }

    private void applyChanges() {
        acceptedChanges.forEach(change -> change.makeChange(database, ce));
        ce.end();
    }
}
