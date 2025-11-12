package org.jabref.gui.util;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.EntryImportHandlerTracker;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileTransferHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.Nullable;

public class InsertUtil {

    public static void addEntriesWithFeedback(@Nullable BibDatabaseContext sourceDatabaseContext,
                                              List<BibEntry> entriesToAdd,
                                              BibDatabaseContext targetDatabaseContext,
                                              String successMessage,
                                              String partialMessage,
                                              DialogService dialogService,
                                              FilePreferences filePreferences,
                                              ImportHandler importHandler,
                                              StateManager stateManager
    ) {
        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, entriesToAdd.size());
        tracker.setOnFinish(() -> {
            int importedCount = tracker.getImportedCount();
            int skippedCount = tracker.getSkippedCount();

            String targetName = targetDatabaseContext.getDatabasePath()
                                                     .map(path -> path.getFileName().toString())
                                                     .orElse(Localization.lang("target library"));

            if (importedCount == entriesToAdd.size()) {
                dialogService.notify(Localization.lang(successMessage, String.valueOf(importedCount), targetName));
            } else if (importedCount == 0) {
                dialogService.notify(Localization.lang("No entry was copied to %0", targetName));
            } else {
                dialogService.notify(Localization.lang(partialMessage, String.valueOf(importedCount), targetName, String.valueOf(skippedCount)));
            }
            if (sourceDatabaseContext != null) {
                LinkedFileTransferHelper
                        .adjustLinkedFilesForTarget(sourceDatabaseContext, targetDatabaseContext, filePreferences);
            }
        });

        importHandler.importEntriesWithDuplicateCheck(targetDatabaseContext, entriesToAdd, tracker);
    }
}
