package org.jabref.gui.search;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class GlobalSearchResultDialog {

    private final ExternalFileTypes externalFileTypes;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final KeyBindingRepository keybindingRepo;
    private final BibDatabaseContext context;
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final FieldColumn libColumn;

    public GlobalSearchResultDialog(BibDatabaseContext context, PreferencesService preferencesService, StateManager stateManager, ExternalFileTypes externalFileTypes, KeyBindingRepository repo, LibraryTab tab, DialogService dialogService) {
        this.context = context;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.externalFileTypes = externalFileTypes;
        this.keybindingRepo = repo;
        this.libraryTab = tab;
        this.dialogService = dialogService;
        this.libColumn = new FieldColumn(MainTableColumnModel.parse("field:library"));

    }

    void showMainTable(BibDatabaseContext ctx) {
        MainTableDataModel model = new MainTableDataModel(ctx, preferencesService, stateManager);

        SearchResultsTable m = new SearchResultsTable(model, ctx, preferencesService, libraryTab, dialogService, stateManager, externalFileTypes, keybindingRepo);

        m.getColumns().add(0, libColumn);
        // m.getColumns().add(0,);

        DialogPane pane = new DialogPane();
        pane.setContent(m);

        dialogService.showCustomDialogAndWait("Global search", pane, ButtonType.OK);
        m.getColumns().remove(libColumn);
    }
}
