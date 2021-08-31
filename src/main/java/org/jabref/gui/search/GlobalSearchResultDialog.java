package org.jabref.gui.search;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class GlobalSearchResultDialog {

    private final ExternalFileTypes externalFileTypes;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final BibDatabaseContext context;
    private final DialogService dialogService;
    private final FieldColumn libColumn;
    private final UndoManager undoManager;

    public GlobalSearchResultDialog(PreferencesService preferencesService, StateManager stateManager, ExternalFileTypes externalFileTypes, UndoManager undoManager, DialogService dialogService) {
        this.context = new BibDatabaseContext();
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.externalFileTypes = externalFileTypes;
        this.undoManager = undoManager;
        this.dialogService = dialogService;
        this.libColumn = new FieldColumn(MainTableColumnModel.parse("field:library"));

    }

    void showMainTable() {

        MainTableDataModel model = new MainTableDataModel(context, preferencesService, stateManager);
        SearchResultsTable m = new SearchResultsTable(model, context, preferencesService, undoManager, dialogService, stateManager, externalFileTypes);

        m.getColumns().add(0, libColumn);

        DialogPane pane = new DialogPane();
        pane.setContent(m);

        dialogService.showNonModalCustomDialogAndWait("Global search", pane, ButtonType.OK);
    }

    void addEntriesToBibContext(BibDatabaseContext ctx) {
        var tbremoved = this.context.getDatabase().getEntries();
        this.context.getDatabase().removeEntries(tbremoved);
        this.context.getDatabase().insertEntries(ctx.getEntries());
    }
}
