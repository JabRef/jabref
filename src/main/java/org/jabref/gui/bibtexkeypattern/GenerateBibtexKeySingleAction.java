package org.jabref.gui.bibtexkeypattern;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GenerateBibtexKeySingleAction extends SimpleCommand {

    private DialogService dialogService;
    private BibDatabaseContext databaseContext;
    private EntryEditorPreferences preferences;
    private BibEntry entry;
    private UndoManager undoManager;

    public GenerateBibtexKeySingleAction(BibEntry entry, BibDatabaseContext databaseContext, DialogService dialogService, EntryEditorPreferences preferences, UndoManager undoManager) {
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.undoManager = undoManager;

        if (preferences.avoidOverwritingCiteKey()) {
            // Only make command executable if cite key is empty
            this.executable.bind(entry.getCiteKeyBinding().isNull());
        }
    }

    @Override
    public void execute() {
        if (!entry.hasCiteKey() || GenerateBibtexKeyAction.confirmOverwriteKeys(dialogService)) {
            new BibtexKeyGenerator(databaseContext, preferences.getBibtexKeyPatternPreferences())
                    .generateAndSetKey(entry)
                    .ifPresent(change -> undoManager.addEdit(new UndoableKeyChange(change)));
        }
    }
}
