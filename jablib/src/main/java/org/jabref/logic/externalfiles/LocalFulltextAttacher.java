package org.jabref.logic.externalfiles;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Attaches a local `file:` PDF (e.g. one written by a browser-extension companion fetcher) to an
/// entry: it copies or moves the file into the library's file directory and renames it per the
/// file-naming pattern, mirroring the fate of a completed HTTP download.
public final class LocalFulltextAttacher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFulltextAttacher.class);

    private LocalFulltextAttacher() {
    }

    public static void attach(URL fileUrl,
                              BibEntry entry,
                              BibDatabaseContext databaseContext,
                              FilePreferences filePreferences,
                              TaskExecutor taskExecutor,
                              NotificationService notificationService) {
        Path sourcePath;
        try {
            sourcePath = Path.of(fileUrl.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            LOGGER.warn("Could not interpret fetcher-returned file URL {}", fileUrl, e);
            notificationService.notify(Localization.lang("No full text document found for entry %0.",
                    entry.getCitationKey().orElse(Localization.lang("undefined"))));
            return;
        }

        LinkedFile linkedFile = new LinkedFile("", sourcePath, StandardFileType.PDF.getName());
        if (entry.getFiles().contains(linkedFile)) {
            notificationService.notify(Localization.lang("Full text document for entry %0 already linked.",
                    entry.getCitationKey().orElse(Localization.lang("undefined"))));
            return;
        }

        LinkedFileHandler handler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        // copyOrMoveToDefaultDirectory does blocking filesystem I/O; run it off the calling thread.
        // shouldMove=true, shouldRenameToFilenamePattern=true — same fate a successful HTTP download
        // would have via DownloadLinkedFileAction.
        BackgroundTask
                .wrap(() -> {
                    handler.copyOrMoveToDefaultDirectory(true, true);
                    return linkedFile;
                })
                .onSuccess(entry::addFile)
                .onFailure(e -> {
                    LOGGER.warn("Could not move fetcher-returned file {} into the library directory",
                            sourcePath, e);
                    entry.addFile(linkedFile);
                })
                .executeWith(taskExecutor);
    }
}
