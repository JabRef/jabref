package org.jabref.gui.search;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableColumnModel.Type;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class GlobalSearchResultDialog {

    public static String LIBRARY_NAME_FIELD = "Library_Name";

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
        this.libColumn = new FieldColumn(new MainTableColumnModel(Type.NORMALFIELD, LIBRARY_NAME_FIELD));
    }

    Optional<ButtonType> showMainTable() {

        MainTableDataModel model = new MainTableDataModel(context, preferencesService, stateManager);
        SearchResultsTable researchTable = new SearchResultsTable(model, context, preferencesService, undoManager, dialogService, stateManager, externalFileTypes);

        researchTable.getColumns().add(0, libColumn);
        researchTable.getColumns().removeIf(col -> col instanceof SpecialFieldColumn);

        DialogPane pane = new DialogPane();
        pane.setContent(researchTable);

        return dialogService.showNonModalCustomDialogAndWait(Localization.lang("Global search"), pane, ButtonType.OK);
    }

    void addEntriesToBibContext(BibDatabaseContext ctx) {
        var tbremoved = this.context.getDatabase().getEntries();
        this.context.getDatabase().removeEntries(tbremoved);
        this.context.getDatabase().insertEntries(ctx.getEntries());
    }
}
