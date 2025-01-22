package org.jabref.gui.edit;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyTo extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyMoreAction.class);

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final CopyToPreferences copyToPreferences;
    private final LibraryTab libraryTab;
    private final BibDatabaseContext sourceDatabaseContext;
    private final BibDatabaseContext targetDatabaseContext;

    public CopyTo(DialogService dialogService,
                  StateManager stateManager,
                  CopyToPreferences copyToPreferences,
                  LibraryTab libraryTab,
                  BibDatabaseContext sourceDatabaseContext,
                  BibDatabaseContext targetDatabaseContext) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.copyToPreferences = copyToPreferences;
        this.libraryTab = libraryTab;
        this.sourceDatabaseContext = sourceDatabaseContext;
        this.targetDatabaseContext = targetDatabaseContext;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        boolean includeCrossReferences = askForCrossReferencedEntries();
        copyToPreferences.setShouldIncludeCrossReferences(includeCrossReferences);

        copyEntryToAnotherLibrary(sourceDatabaseContext, targetDatabaseContext);
    }

     public void copyEntryToAnotherLibrary(BibDatabaseContext sourceDatabaseContext, BibDatabaseContext targetDatabaseContext) {
        List<BibEntry> selectedEntries = stateManager.getSelectedEntries();

        targetDatabaseContext.getDatabase().insertEntries(selectedEntries);
    }

    private boolean askForCrossReferencedEntries() {
        if (copyToPreferences.getShouldAskForIncludingCrossReferences()) {
            return dialogService.showConfirmationDialogWithOptOutAndWait(
                    Localization.lang("Include or exclude cross-referenced entries"),
                    Localization.lang("Would you like to include cross-reference entries in the current operation?"),
                    Localization.lang("Include"),
                    Localization.lang("Exclude"),
                    Localization.lang("Do not ask again"),
                    optOut -> copyToPreferences.setShouldAskForIncludingCrossReferences(!optOut)
            );
        } else {
            return copyToPreferences.getShouldIncludeCrossReferences();
        }
    }
}
