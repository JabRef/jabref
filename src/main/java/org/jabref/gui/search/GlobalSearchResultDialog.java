package org.jabref.gui.search;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class GlobalSearchResultDialog{

    private final ExternalFileTypes externalFileTypes;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final KeyBindingRepository keybindingRepo;
    private final BibDatabaseContext context;
    private final LibraryTab libraryTab;

    public GlobalSearchResultDialog(BibDatabaseContext context, PreferencesService preferencesService, StateManager stateManager, ExternalFileTypes externalFileTypes, KeyBindingRepository repo, LibraryTab tab  ){
        this.context = context;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.externalFileTypes = externalFileTypes;
        this.keybindingRepo = repo;
        this.libraryTab = tab;


    }

    void showMainTable(){
        MainTableDataModel model = new MainTableDataModel(context, preferencesService, stateManager);
        MainTable m = new  MainTable(model, null, context, preferencesService, null, stateManager, null, null);

    }
}
