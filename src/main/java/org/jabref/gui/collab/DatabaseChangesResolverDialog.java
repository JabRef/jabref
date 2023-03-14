package org.jabref.gui.collab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseChangesResolverDialog extends BaseDialog<Boolean> {
    private final static Logger LOGGER = LoggerFactory.getLogger(DatabaseChangesResolverDialog.class);
    /**
     * Reconstructing the details view to preview an {@link DatabaseChange} every time it's selected is a heavy operation.
     * It is also useless because changes are static and if the change data is static then the view doesn't have to change
     * either. This cache is used to ensure that we only create the detail view instance once for each {@link DatabaseChange}.
     */
    private final Map<DatabaseChange, DatabaseChangeDetailsView> DETAILS_VIEW_CACHE = new HashMap<>();

    @FXML
    private TableView<DatabaseChange> changesTableView;
    @FXML
    private TableColumn<DatabaseChange, String> changeName;
    @FXML
    private Button askUserToResolveChangeButton;
    @FXML
    private BorderPane changeInfoPane;

    private final List<DatabaseChange> changes;
    private final BibDatabaseContext database;

    private ExternalChangesResolverViewModel viewModel;

    @Inject private UndoManager undoManager;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferencesService;
    @Inject private ThemeManager themeManager;
    @Inject private BibEntryTypesManager entryTypesManager;

    /**
     * A dialog going through given <code>changes</code>, which are diffs to the provided <code>database</code>.
     * Each accepted change is written to the provided <code>database</code>.
     *
     * @param changes The list of changes
     * @param database The database to apply the changes to
     */
    public DatabaseChangesResolverDialog(List<DatabaseChange> changes, BibDatabaseContext database, String dialogTitle) {
        this.changes = changes;
        this.database = database;

        this.setTitle(dialogTitle);
        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        this.setResultConverter(button -> {
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
        PreviewViewer previewViewer = new PreviewViewer(database, dialogService, stateManager, themeManager);
        DatabaseChangeDetailsViewFactory databaseChangeDetailsViewFactory = new DatabaseChangeDetailsViewFactory(database, dialogService, stateManager, themeManager, preferencesService, entryTypesManager, previewViewer);

        viewModel = new ExternalChangesResolverViewModel(changes, undoManager);

        changeName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        askUserToResolveChangeButton.disableProperty().bind(viewModel.canAskUserToResolveChangeProperty().not());

        changesTableView.setItems(viewModel.getVisibleChanges());
        // Think twice before setting this to MULTIPLE...
        changesTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        changesTableView.getSelectionModel().selectFirst();

        viewModel.selectedChangeProperty().bind(changesTableView.getSelectionModel().selectedItemProperty());
        EasyBind.subscribe(viewModel.selectedChangeProperty(), selectedChange -> {
            if (selectedChange != null) {
                DatabaseChangeDetailsView detailsView = DETAILS_VIEW_CACHE.computeIfAbsent(selectedChange, databaseChangeDetailsViewFactory::create);
                changeInfoPane.setCenter(detailsView);
            }
        });

        EasyBind.subscribe(viewModel.areAllChangesResolvedProperty(), isResolved -> {
            if (isResolved) {
                viewModel.applyChanges();
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
    public void askUserToResolveChange() {
        viewModel.getSelectedChange().flatMap(DatabaseChange::getExternalChangeResolver)
                 .flatMap(DatabaseChangeResolver::askUserToResolveChange).ifPresent(viewModel::acceptMergedChange);
    }
}
