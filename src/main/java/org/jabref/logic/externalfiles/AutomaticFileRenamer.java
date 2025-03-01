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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticFileRenamer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticFileRenamer.class);
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final AtomicBoolean isCurrentlyRenamingFile = new AtomicBoolean(false);

    public AutomaticFileRenamer(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
        LOGGER.debug("AutomaticFileRenamer instance created [DB:{}]", databaseContext.getLocation());
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        LOGGER.debug("===== Field change event detected =====");
        LOGGER.debug("Changed field: {}", event.getField());
        LOGGER.debug("Corresponding entry: {}", event.getBibEntry().getCitationKey().orElse("no citation key"));
        LOGGER.debug("New value: {}", event.getNewValue());

        // Check preference setting first
        if (!filePreferences.shouldAutoRenameFilesOnEntryChange()) {
            LOGGER.debug("Auto rename files on entry change is disabled, ignoring event");
            return;
        }

        // Avoid reentrance
        if (isCurrentlyRenamingFile.get()) {
            LOGGER.debug("Already processing file renaming, ignoring new event: {}", event);
            return;
        }

        // Add debug output
        LOGGER.debug("Received field change event: {} -> '{}' [DB:{}]",
                event.getField(),
                event.getNewValue(),
                databaseContext.getLocation());

        // Execute renaming for any field change, not just limited to Citation Key or related fields
        // If it's a relevant field change, trigger renaming with delay
        BibEntry entry = event.getBibEntry();
        LOGGER.debug("Current entry files: {}", entry.getFiles());

        // If the entry has no associated files, no need to rename
        if (entry.getFiles().isEmpty()) {
            LOGGER.debug("Entry has no associated files, no need to rename");
            return;
        }

        // To ensure UI updates and other file operations have priority, use a short delay for renaming
        new Thread(() -> {
            try {
                // Wait a few hundred milliseconds to let higher priority operations execute first
                Thread.sleep(500);

                LOGGER.debug("Starting automatic file renaming...");

                // Add synchronization lock to avoid concurrent renaming
                if (!isCurrentlyRenamingFile.compareAndSet(false, true)) {
                    LOGGER.debug("Another thread is already performing renaming, exiting current thread");
                    return;
                }

                try {
                    LOGGER.debug("Starting renaming execution...");

                    // First check if the entry has a citation key, if not don't perform renaming
                    if (!entry.getCitationKey().isPresent() && entry.getFiles().size() > 0) {
                        LOGGER.debug("Entry has no citation key but has associated files, not performing renaming");
                    } else {
                        // Save the original file list for comparison
                        List<LinkedFile> originalFiles = new ArrayList<>(entry.getFiles());
                        List<LinkedFile> updatedFiles = new ArrayList<>();
                        boolean anyFileRenamed = false;
                        
                        // Iterate through all files and rename them
                        for (LinkedFile linkedFile : entry.getFiles()) {
                            if (linkedFile.isOnlineLink()) {
                                updatedFiles.add(linkedFile); // Keep online links unchanged
                                continue;
                            }

                            Optional<Path> filePath = linkedFile.findIn(databaseContext, filePreferences);
                            if (filePath.isEmpty()) {
                                updatedFiles.add(linkedFile); // Cannot find file path, keep unchanged
                                continue;
                            }

                            // Use LinkedFileHandler to perform the actual renaming operation
                            LinkedFileHandler fileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);
                            try {
                                boolean renamed = fileHandler.renameToSuggestedName();
                                if (renamed) {
                                    // Get the updated link
                                    LinkedFile updatedFile = fileHandler.refreshFileLink();
                                    updatedFiles.add(updatedFile);
                                    anyFileRenamed = true;
                                    LOGGER.debug("Successfully renamed file to: {}", updatedFile.getLink());
                                } else {
                                    updatedFiles.add(linkedFile); // Renaming failed, keep unchanged
                                }
                            } catch (Exception e) {
                                LOGGER.error("Error while renaming file", e);
                                updatedFiles.add(linkedFile); // Error occurred, keep unchanged
                            }
                        }
                        
                        // Only update the entry when the file list has changed
                        if (anyFileRenamed) {
                            LOGGER.debug("Files have been renamed, updating BibEntry's file list");
                            entry.setFiles(updatedFiles);
                            LOGGER.debug("Updated file list: {}", entry.getFiles());
                        }
                    }

                    LOGGER.debug("Renaming execution completed");
                } finally {
                    // Release the lock regardless of whether renaming was successful
                    isCurrentlyRenamingFile.set(false);
                }
            } catch (Exception e) {
                LOGGER.error("Automatic file renaming failed", e);
            }
        }).start();
    }

    // Add public method for manual triggering
    public void renameAssociatedFiles(BibEntry entry) {
        // Set renaming state flag
        if (!isCurrentlyRenamingFile.compareAndSet(false, true)) {
            LOGGER.debug("Already processing file renaming, ignoring entry processing: {}", entry.getId());
            return;
        }

        try {
            LOGGER.debug("Starting to rename associated files...");

            // Get all files associated with the entry
            List<LinkedFile> oldFiles = entry.getFiles();
            if (oldFiles.isEmpty()) {
                LOGGER.debug("Entry has no associated files, no need to rename: {}", entry.getId());
                return;
            }

            List<LinkedFile> newFiles = new ArrayList<>();
            boolean anyRenamed = false;

            LOGGER.debug("Starting to process entry: {}", entry.getCitationKey().orElse("no citation key"));

            List<LinkedFile> linkedFiles = entry.getFiles();
            LOGGER.debug("Current entry has {} files", linkedFiles.size());

            if (linkedFiles.isEmpty()) {
                LOGGER.debug("No files need to be renamed");
                return;
            }

            // Use the same logic as "Rename file to defined pattern" in UI for renaming
            List<Optional<LinkedFile>> renamedFiles = new ArrayList<>(linkedFiles.size());

            // Rename all files
            for (LinkedFile linkedFile : linkedFiles) {
                LOGGER.debug("Processing file: {}", linkedFile.getLink());

                if (linkedFile.isOnlineLink()) {
                    LOGGER.debug("File is an online link, not renaming: {}", linkedFile.getLink());
                    renamedFiles.add(Optional.of(linkedFile));
                    continue;
                }

                // Check if the file can be found in the physical path
                Optional<Path> filePath = linkedFile.findIn(databaseContext, filePreferences);

                if (filePath.isEmpty()) {
                    LOGGER.debug("File path not found, skipping renaming: {}", linkedFile.getLink());
                    renamedFiles.add(Optional.of(linkedFile));
                    continue;
                }

                LOGGER.debug("File physical path: {}", filePath.get());

                // Create handler and use the same renaming logic as UI
                LinkedFileHandler fileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

                try {
                    // Get suggested file name
                    String suggestedFileName = fileHandler.getSuggestedFileName();
                    LOGGER.debug("Suggested file name based on pattern: {}", suggestedFileName);

                    // Check if file name is the same
                    Path oldFilePath = filePath.get();
                    String oldFileName = oldFilePath.getFileName().toString();

                    if (oldFileName.equals(suggestedFileName)) {
                        LOGGER.debug("File name already matches pattern, no need to rename: {}", oldFileName);
                        renamedFiles.add(Optional.of(linkedFile));
                        continue;
                    }

                    // Perform actual renaming operation
                    LOGGER.debug("Renaming file {} to {}", oldFileName, suggestedFileName);
                    try {
                        boolean renameSuccess = fileHandler.renameToName(suggestedFileName, false);

                        if (renameSuccess) {
                            LOGGER.debug("Renaming successful: {} -> {}", oldFileName, suggestedFileName);
                            anyRenamed = true;
                            // Update link - refreshFileLink updates the file's relative path
                            renamedFiles.add(Optional.of(fileHandler.refreshFileLink()));
                        } else {
                            LOGGER.debug("Renaming failed, keeping original file: {}", oldFileName);
                            renamedFiles.add(Optional.of(linkedFile));
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error during renaming process", e);
                        renamedFiles.add(Optional.of(linkedFile));
                    }
                } catch (Exception exception) {
                    LOGGER.error("Exception occurred during renaming: {}", exception.getMessage());
                    exception.printStackTrace();
                    renamedFiles.add(Optional.of(linkedFile));
                }
            }

            // If needed, completely rebuild the list
            List<LinkedFile> result = new ArrayList<>(linkedFiles.size());
            for (int i = 0; i < linkedFiles.size(); i++) {
                final int index = i; // Use final variable to solve lambda expression reference issue
                renamedFiles.get(index).ifPresentOrElse(result::add, () -> result.add(linkedFiles.get(index)));
            }
            LOGGER.debug("Finally returning {} files", result.size());

            // Only update BibEntry when files were actually renamed
            if (!result.equals(oldFiles)) {
                LOGGER.debug("Files were renamed, updating BibEntry file links");
                entry.setFiles(result);
                LOGGER.debug("Updated entry files: {}", entry.getFiles());
            } else {
                LOGGER.debug("No files were renamed, BibEntry remains unchanged");
            }
        } finally {
            // Reset renaming state flag
            isCurrentlyRenamingFile.set(false);
        }
    }
}
