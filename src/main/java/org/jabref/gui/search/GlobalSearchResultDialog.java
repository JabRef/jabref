package org.jabref.gui.search;

import javax.inject.Inject;
import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class GlobalSearchResultDialog extends BaseDialog<Void> {

    private final ExternalFileTypes externalFileTypes;

    private final BibDatabaseContext context;
    private final UndoManager undoManager;

    @FXML private TableView<BibEntryTableViewModel> resultsTable;
    @FXML private Pane preview;

    @Inject private PreferencesService preferencesService;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;

    private GlobalSearchResultDialogViewModel viewModel;

    public GlobalSearchResultDialog(ExternalFileTypes externalFileTypes, UndoManager undoManager) {
        this.undoManager = undoManager;
        this.externalFileTypes = externalFileTypes;
        this.context = new BibDatabaseContext();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        // viewModel = new GlobalSearchResultDialogViewModel(stateManager);

        PreviewViewer previewViewer = new PreviewViewer(context, dialogService, stateManager);
        previewViewer.setTheme(preferencesService.getTheme());
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getCurrentPreviewStyle());
        preview.getChildren().add(previewViewer);

        FieldColumn fieldColumn = new FieldColumn(MainTableColumnModel.parse("field:library"));
        new ValueTableCellFactory<BibEntryTableViewModel, String>()
                .withText(FileUtil::getBaseName)
                .install(fieldColumn);

        MainTableDataModel model = new MainTableDataModel(context, preferencesService, stateManager);
        resultsTable = new SearchResultsTable(model, context, preferencesService, undoManager, dialogService, stateManager, externalFileTypes);
        resultsTable.getColumns().add(0, fieldColumn);
        resultsTable.getColumns().removeIf(col -> col instanceof SpecialFieldColumn);
        resultsTable.getSelectionModel().selectFirst();

        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                previewViewer.setEntry(newValue.getEntry());
            } else {
                previewViewer.setEntry(old.getEntry());
            }
        });

        // resultsTable.setItems(viewModel);
    }

    public void updateSearch() {
        // viewModel.updateSearch();
    }

}
