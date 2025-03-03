package org.jabref.logic.externalfiles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;

import com.google.common.eventbus.Subscribe;

public class AutomaticFileRenamer {

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final AtomicBoolean isCurrentlyRenamingFile = new AtomicBoolean(false);

    public AutomaticFileRenamer(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (!filePreferences.shouldAutoRenameFilesOnEntryChange()) {
            return;
        }

        if (isCurrentlyRenamingFile.get()) {
            return;
        }

        BibEntry entry = event.getBibEntry();

        if (entry.getFiles().isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(500);

                if (!isCurrentlyRenamingFile.compareAndSet(false, true)) {
                    return;
                }

                try {
                    if (entry.getCitationKey().isEmpty() && !entry.getFiles().isEmpty()) {
                        // No citation key but has files
                    } else {
                        List<LinkedFile> originalFiles = new ArrayList<>(entry.getFiles());
                        List<LinkedFile> updatedFiles = new ArrayList<>();
                        boolean anyFileRenamed = false;

                        for (LinkedFile linkedFile : entry.getFiles()) {
                            if (linkedFile.isOnlineLink()) {
                                updatedFiles.add(linkedFile);
                                continue;
                            }

                            Optional<Path> filePath = linkedFile.findIn(databaseContext, filePreferences);
                            if (filePath.isEmpty()) {
                                updatedFiles.add(linkedFile);
                                continue;
                            }

                            LinkedFileHandler fileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);
                            try {
                                boolean renamed = fileHandler.renameToSuggestedName();
                                if (renamed) {
                                    LinkedFile updatedFile = fileHandler.refreshFileLink();
                                    updatedFiles.add(updatedFile);
                                    anyFileRenamed = true;
                                } else {
                                    updatedFiles.add(linkedFile);
                                }
                            } catch (Exception e) {
                                updatedFiles.add(linkedFile);
                            }
                        }

                        if (anyFileRenamed) {
                            entry.setFiles(updatedFiles);
                        }
                    }
                } finally {
                    isCurrentlyRenamingFile.set(false);
                }
            } catch (Exception e) {
                // Silent catch
            }
        }).start();
    }
}
