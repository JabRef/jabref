package org.jabref.gui.bibtexkeypattern;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class GenerateBibtexKeySingleAction extends SimpleCommand {

    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final BibEntry entry;
    private final UndoManager undoManager;

    public GenerateBibtexKeySingleAction(BibEntry entry, BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferencesService, UndoManager undoManager) {
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.undoManager = undoManager;

        if (preferencesService.getBibtexKeyPatternPreferences().shouldAvoidOverwriteCiteKey()) {
            this.executable.bind(entry.getCiteKeyBinding().isEmpty());
        }
    }

    @Override
    public void execute() {
        if (!entry.hasCiteKey() || GenerateBibtexKeyAction.confirmOverwriteKeys(dialogService)) {
            new BibtexKeyGenerator(databaseContext, preferencesService.getBibtexKeyPatternPreferences())
                    .generateAndSetKey(entry)
                    .ifPresent(change -> undoManager.addEdit(new UndoableKeyChange(change)));
        }
    }
}
