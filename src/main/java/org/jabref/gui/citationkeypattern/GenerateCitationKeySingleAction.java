package org.jabref.gui.citationkeypattern;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class GenerateCitationKeySingleAction extends SimpleCommand {

    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferencesService;
    private final BibEntry entry;
    private final UndoManager undoManager;

    public GenerateCitationKeySingleAction(BibEntry entry, BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferencesService, UndoManager undoManager) {
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.undoManager = undoManager;

        if (preferencesService.getCitationKeyPatternPreferences().shouldAvoidOverwriteCiteKey()) {
            this.executable.bind(entry.getCiteKeyBinding().isEmpty());
        }
    }

    @Override
    public void execute() {
        if (!entry.hasCitationKey() || GenerateCitationKeyAction.confirmOverwriteKeys(dialogService, preferencesService)) {
            new CitationKeyGenerator(databaseContext, preferencesService.getCitationKeyPatternPreferences())
                    .generateAndSetKey(entry)
                    .ifPresent(change -> undoManager.addEdit(new UndoableKeyChange(change)));
        }
    }
}
