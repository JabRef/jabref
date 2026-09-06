package org.jabref.gui.util;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.EntryImportHandlerTracker;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.TransferInformation;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class InsertUtil {

    /// Renders a user feedback message for a given number of entries.
    ///
    /// Implementations have to call [Localization#lang(String, Object...)] with a **literal** key,
    /// because the localization consistency tests can only detect literal keys.
    @FunctionalInterface
    @NullMarked
    public interface FeedbackMessage {
        /// @param params Replacement strings for the parameters %0, %1, etc. of the message
        String format(Object... params);
    }

    /// @param jabRefClipboardTransferData - can be null if called via clipboard and clipboard content was NOT created by JabRef
    public static void addEntriesWithFeedback(@Nullable TransferInformation jabRefClipboardTransferData,
                                              List<BibEntry> entriesToAdd,
                                              BibDatabaseContext targetDatabaseContext,
                                              FeedbackMessage successMessage,
                                              FeedbackMessage partialMessage,
                                              DialogService dialogService,
                                              ImportHandler importHandler,
                                              StateManager stateManager
    ) {
        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, targetDatabaseContext, entriesToAdd.size());
        tracker.setOnFinish(() -> {
            int importedCount = tracker.getImportedCount();
            int skippedCount = tracker.getSkippedCount();

            String targetName = targetDatabaseContext.getDatabasePath()
                                                     .map(path -> path.getFileName().toString())
                                                     .orElse(Localization.lang("target library"));

            if (importedCount == entriesToAdd.size()) {
                dialogService.notify(successMessage.format(importedCount, targetName));
            } else if (importedCount == 0) {
                dialogService.notify(Localization.lang("No entry was copied to %0", targetName));
            } else {
                dialogService.notify(partialMessage.format(importedCount, targetName, skippedCount));
            }
        });

        importHandler.importEntriesWithDuplicateCheck(jabRefClipboardTransferData, entriesToAdd, tracker);
    }
}
