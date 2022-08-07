package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.undo.NamedCompound;
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
    private final NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));

    private final BooleanBinding areChangesResolved;

    private final BooleanBinding canOpenAdvancedMergeDialog;

    private final List<DatabaseChangeViewModel> changes;

    public ExternalChangesResolverDialog(BibDatabaseContext database, List<DatabaseChangeViewModel> changes) {
        this.database = database;
        this.changes = new ArrayList<>(changes);

        this.setTitle(Localization.lang("External changes"));
        this.getDialogPane().setPrefSize(800, 600);

        ViewLoader.view(this)
                .load().setAsDialogPane(this);
        changesTableView.itemsProperty().set(FXCollections.observableList(changes));
        changesTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        changesTableView.getSelectionModel().selectFirst();

        areChangesResolved = Bindings.createBooleanBinding(() -> changesTableView.getItems().isEmpty(), changesTableView.itemsProperty().getValue());
        EasyBind.subscribe(areChangesResolved, isResolved -> {
            if (isResolved) {
                LOGGER.info("Closing ExternalChangesResolverDialog");
                close();
            }
        });

        canOpenAdvancedMergeDialog = Bindings.createBooleanBinding(() -> changesTableView.getSelectionModel().getSelectedItems().size() == 1 &&
                changesTableView.getSelectionModel().getSelectedItems().get(0).hasAdvancedMergeDialog(), changesTableView.getSelectionModel().getSelectedItems());

        openAdvancedMergeDialogButton.disableProperty().bind(canOpenAdvancedMergeDialog.not());

        setResultConverter(button -> {
            if (areChangesResolved.get()) {
                LOGGER.info("External changes are resolved successfully");
                Platform.runLater(this::applyChanges);
                // TODO: panel.getUndoManager().addEdit(ce);
                return true;
            } else {
                LOGGER.info("External changes ARE NOT resolved");
                return false;
            }
        });
        EasyBind.subscribe(changesTableView.getSelectionModel().selectedItemProperty(), selectedChange -> {
            if (selectedChange != null) {
                changeInfoPane.setCenter(selectedChange.description());
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
        changesTableView.getSelectionModel().getSelectedItems().forEach(DatabaseChangeViewModel::accept);
        changesTableView.getItems().removeAll(changesTableView.getSelectionModel().getSelectedItems());
    }

    @FXML
    public void openAdvancedMergeDialog() {
        assert changesTableView.getSelectionModel().getSelectedItems().size() == 1;
        assert changesTableView.getSelectionModel().getSelectedItems().get(0).hasAdvancedMergeDialog();

        DatabaseChangeViewModel changeViewModel = changesTableView.getSelectionModel().getSelectedItems().get(0);
        changeViewModel.openAdvancedMergeDialog().ifPresent(change -> {
            change.accept();
            changesTableView.getItems().removeAll(changeViewModel);
        });
    }

    private void applyChanges() {
        changes.stream().filter(DatabaseChangeViewModel::isAccepted).forEach(change -> change.makeChange(database, ce));
        ce.end();
    }
}
