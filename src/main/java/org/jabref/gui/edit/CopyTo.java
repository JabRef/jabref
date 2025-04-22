package org.jabref.gui.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyTo extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyTo.class);

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final CopyToPreferences copyToPreferences;
    private final ImportHandler importHandler;
    private final BibDatabaseContext sourceDatabaseContext;
    private final BibDatabaseContext targetDatabaseContext;

    private static final String DEFAULT_TARGET_LIBRARY_NAME = "target library";


    public CopyTo(DialogService dialogService,
                  StateManager stateManager,
                  CopyToPreferences copyToPreferences,
                  ImportHandler importHandler,
                  BibDatabaseContext sourceDatabaseContext,
                  BibDatabaseContext targetDatabaseContext) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.copyToPreferences = copyToPreferences;
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
            int before = targetDatabaseContext.getEntries().size();
            copyEntriesWithCrossRef(selectedEntries, targetDatabaseContext);
            dialogService.notify(Localization.lang("Entries copied successfully, including cross-references."));
            int after = targetDatabaseContext.getEntries().size();
            if (after == before) {
                dialogService.notify(Localization.lang("No new entries were added to the target library."));
                return;
            }

            String targetLibraryName = targetDatabaseContext.getDatabasePath()
                    .map(path -> path.getFileName().toString())
                    .orElse(DEFAULT_TARGET_LIBRARY_NAME);

            dialogService.notify(Localization.lang("Copied %0 entries to '%1'", String.valueOf(after - before), targetLibraryName));
        } else {
            int before = targetDatabaseContext.getEntries().size();
            copyEntriesWithoutCrossRef(selectedEntries, targetDatabaseContext);
            int after = targetDatabaseContext.getEntries().size();
            if (after > before) {
                String targetLibraryName = targetDatabaseContext.getDatabasePath()
                        .map(path -> path.getFileName().toString())
                        .orElse("target library");

                dialogService.notify(Localization.lang("Entries copied successfully to %0, without cross-references.", targetLibraryName));
            }
        }
    }

    public void copyEntriesWithCrossRef(List<BibEntry> selectedEntries, BibDatabaseContext targetDatabaseContext) {
        List<BibEntry> entriesToAdd = new ArrayList<>(selectedEntries);

        List<BibEntry> entriesWithCrossRef = selectedEntries.stream().filter(bibEntry -> bibEntry.hasField(StandardField.CROSSREF))
                                                            .flatMap(entry -> getCrossRefEntry(entry, sourceDatabaseContext).stream()).toList();
        entriesToAdd.addAll(entriesWithCrossRef);
        importHandler.importEntriesWithDuplicateCheck(targetDatabaseContext, entriesToAdd);
    }

    public void copyEntriesWithoutCrossRef(List<BibEntry> selectedEntries, BibDatabaseContext targetDatabaseContext) {
        importHandler.importEntriesWithDuplicateCheck(targetDatabaseContext, selectedEntries);
    }

    public Optional<BibEntry> getCrossRefEntry(BibEntry bibEntryToCheck, BibDatabaseContext sourceDatabaseContext) {
        return bibEntryToCheck.getField(StandardField.CROSSREF)
                .flatMap(crossRefKey -> sourceDatabaseContext.getEntries().stream()
                        .filter(entry -> entry.getCitationKey().isPresent() && entry.getCitationKey().get().equals(crossRefKey))
                        .findFirst());
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
