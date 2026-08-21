package org.jabref.gui.citationkeypattern;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoManager;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.change.UndoableFieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GenerateCitationKeySingleAction extends SimpleCommand {

    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final CliPreferences preferences;
    private final BibEntry entry;
    private final UndoManager undoManager;

    public GenerateCitationKeySingleAction(BibEntry entry, BibDatabaseContext databaseContext, DialogService dialogService, CliPreferences preferences, UndoManager undoManager) {
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.undoManager = undoManager;

        if (preferences.getCitationKeyPatternPreferences().shouldAvoidOverwriteCiteKey()) {
            this.executable.bind(entry.getCiteKeyBinding().isEmpty());
        }
    }

    @Override
    public void execute() {
        if (!entry.hasCitationKey() || GenerateCitationKeyAction.confirmOverwriteKeys(dialogService, preferences)) {
            new CitationKeyGenerator(databaseContext, preferences.getCitationKeyPatternPreferences())
                    .generateAndSetKey(entry)
                    .ifPresent(change -> undoManager.addEdit(new UndoableFieldChange(change)));
        }
    }
}
