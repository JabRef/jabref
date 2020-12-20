package org.jabref.gui.externalfiles;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class ExternalFilesDialogViewModel {

    private final ImportHandler importHandler;

    public ExternalFilesDialogViewModel(DialogService dialogService, BibDatabaseContext databaseContext,
                                        ExternalFileTypes externalFileTypes, UndoManager undoManager,
                                        FileUpdateMonitor fileUpdateMonitor, PreferencesService preferences, StateManager stateManager) {
        importHandler = new ImportHandler(
                                          dialogService,
                                          databaseContext,
                                          externalFileTypes,
                                          preferences,
                                          fileUpdateMonitor,
                                          undoManager,
                                          stateManager);
    }

    public void startImport()
    {

    }

}
