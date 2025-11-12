package org.jabref.gui.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.util.InsertUtil.addEntriesWithFeedback;

// TODO: This is only available in the context menu; and not when using copy and paste.
//       This is an inconsistency in the UI.
//       Should be merged with [org.jabref.gui.LibraryTab#pasteEntry]; maybe integrate in [org.jabref.gui.util.CopyUtil]
public class CopyTo extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyTo.class);

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final CopyToPreferences copyToPreferences;
    private final FilePreferences filePreferences;
    private final ImportHandler importHandler;
    private final BibDatabaseContext sourceDatabaseContext;
    private final BibDatabaseContext targetDatabaseContext;

    public CopyTo(DialogService dialogService,
                  StateManager stateManager,
                  CopyToPreferences copyToPreferences,
                  FilePreferences filePreferences,
                  ImportHandler importHandler,
                  BibDatabaseContext sourceDatabaseContext,
                  BibDatabaseContext targetDatabaseContext) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.copyToPreferences = copyToPreferences;
        this.filePreferences = filePreferences;
        this.importHandler = importHandler;
        this.sourceDatabaseContext = sourceDatabaseContext;
        this.targetDatabaseContext = targetDatabaseContext;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        // we need to operate on a copy otherwise we might get ConcurrentModification issues
        List<BibEntry> selectedEntries = stateManager.getSelectedEntries().stream().toList();

        boolean includeCrossReferences = copyToPreferences.getShouldIncludeCrossReferences();
        boolean showDialogBox = copyToPreferences.getShouldAskForIncludingCrossReferences();

        for (BibEntry bibEntry : selectedEntries) {
            if (bibEntry.hasField(StandardField.CROSSREF) && showDialogBox) {
                includeCrossReferences = askForCrossReferencedEntries();
                copyToPreferences.setShouldIncludeCrossReferences(includeCrossReferences);
                break;
            }
        }

        if (includeCrossReferences) {
            copyEntriesWithCrossRef(selectedEntries, targetDatabaseContext);
        } else {
            copyEntriesWithoutCrossRef(selectedEntries, targetDatabaseContext);
        }
    }

    public void copyEntriesWithCrossRef(List<BibEntry> selectedEntries, BibDatabaseContext targetDatabaseContext) {
        List<BibEntry> entriesToAdd = new ArrayList<>(selectedEntries);

        List<BibEntry> entriesWithCrossRef = selectedEntries.stream().filter(bibEntry -> bibEntry.hasField(StandardField.CROSSREF))
                                                            .flatMap(entry -> getCrossRefEntry(entry, sourceDatabaseContext).stream()).toList();
        entriesToAdd.addAll(entriesWithCrossRef);

        addEntriesWithFeedback(
                sourceDatabaseContext,
                entriesToAdd,
                targetDatabaseContext,
                Localization.lang("Copied %0 entry(s) to %1, including cross-references"),
                Localization.lang("Copied %0 entry(s) to %1. %2 were skipped including cross-references"),
                dialogService,
                filePreferences,
                importHandler,
                stateManager
        );
    }

    public void copyEntriesWithoutCrossRef(List<BibEntry> selectedEntries, BibDatabaseContext targetDatabaseContext) {
        addEntriesWithFeedback(
                sourceDatabaseContext,
                selectedEntries,
                targetDatabaseContext,
                Localization.lang("Copied %0 entry(s) to %1, without cross-references"),
                Localization.lang("Copied %0 entry(s) to %1. %2 were skipped without cross-references"),
                dialogService,
                filePreferences,
                importHandler,
                stateManager
        );
    }

    public Optional<BibEntry> getCrossRefEntry(BibEntry bibEntryToCheck, BibDatabaseContext sourceDatabaseContext) {
        return sourceDatabaseContext.getEntries().stream().filter(entry -> bibEntryToCheck.getField(StandardField.CROSSREF).equals(entry.getCitationKey())).findFirst();
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
