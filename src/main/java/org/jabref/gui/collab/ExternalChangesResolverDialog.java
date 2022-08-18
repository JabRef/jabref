package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.collab.experimental.ExternalChange;
import org.jabref.gui.collab.experimental.ExternalChangeDetailsViewFactory;
import org.jabref.gui.collab.experimental.ExternalChangeResolver;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalChangesResolverDialog extends BaseDialog<Boolean> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalChangesResolverDialog.class);

    @FXML
    private TableView<ExternalChange> changesTableView;
    @FXML
    private TableColumn<ExternalChange, String> changeName;
    @FXML
    private Button askUserToResolveChangeButton;
    @FXML
    private BorderPane changeInfoPane;

    private final BibDatabaseContext database;

    private final List<ExternalChange> changes;

    private ExternalChangesResolverViewModel viewModel;

    private final ExternalChangeDetailsViewFactory externalChangeDetailsViewFactory;

    public ExternalChangesResolverDialog(BibDatabaseContext database, List<ExternalChange> changes) {
        this.database = database;
        this.changes = new ArrayList<>(changes);
        this.externalChangeDetailsViewFactory = new ExternalChangeDetailsViewFactory();

        ViewLoader.view(this)
                .load().setAsDialogPane(this);

        setResultConverter(button -> {
            if (viewModel.areAllChangesResolved()) {
                LOGGER.info("External changes are resolved successfully");
                return true;
            } else {
                LOGGER.info("External changes aren't resolved");
                return false;
            }
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ExternalChangesResolverViewModel(changes);

        changeName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        askUserToResolveChangeButton.disableProperty().bind(viewModel.canAskUserToResolveChangeProperty().not());

        changesTableView.setItems(viewModel.getVisibleChanges());
        changesTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        changesTableView.getSelectionModel().selectFirst();

        viewModel.selectedChangeProperty().bind(changesTableView.getSelectionModel().selectedItemProperty());
        EasyBind.subscribe(viewModel.selectedChangeProperty(), selectedChange -> {
            if (selectedChange != null) {
                changeInfoPane.setCenter(externalChangeDetailsViewFactory.create(selectedChange));
            }
        });

        EasyBind.subscribe(viewModel.areAllChangesResolvedProperty(), isResolved -> {
            if (isResolved) {
                Platform.runLater(viewModel::applyChanges);
                close();
                LOGGER.info("Closing ExternalChangesResolverDialog");
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
    public void askUserToResolveChange() {
        viewModel.getSelectedChange().flatMap(ExternalChange::getExternalChangeResolver)
                 .flatMap(ExternalChangeResolver::askUserToResolveChange).ifPresent(viewModel::acceptMergedChange);
    }
}
