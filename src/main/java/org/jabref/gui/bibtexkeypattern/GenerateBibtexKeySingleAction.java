package org.jabref.gui.bibtexkeypattern;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class GenerateBibtexKeySingleAction extends SimpleCommand {

    private DialogService dialogService;
    private BibDatabaseContext databaseContext;
    private PreferencesService preferencesService;
    private BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private BibEntry entry;
    private UndoManager undoManager;

    public GenerateBibtexKeySingleAction(BibEntry entry, BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferencesService, UndoManager undoManager) {
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.bibtexKeyPatternPreferences = preferencesService.getBibtexKeyPatternPreferences();
        this.undoManager = undoManager;

        if (bibtexKeyPatternPreferences.avoidOverwritingCiteKey()) {
            this.executable.bind(entry.getCiteKeyBinding().isNull());
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
