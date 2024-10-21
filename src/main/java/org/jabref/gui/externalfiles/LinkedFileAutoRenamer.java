package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFileAutoRenamer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileAutoRenamer.class);

    private final GuiPreferences preferences;
    private final BibDatabaseContext bibDatabaseContext;

    public LinkedFileAutoRenamer(BibDatabaseContext bibDatabaseContext, GuiPreferences preferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.preferences = preferences;
        bibDatabaseContext.getDatabase().registerListener(this);
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        LOGGER.debug("FieldChangedEvent triggered for field: {}", event.getField());

        if (!preferences.shouldAutoRenameLinkedFiles()) {
            return;
        }

        BibEntry entry = event.getBibEntry();
        Field changedField = event.getField();

        String fileNamePattern = preferences.getFilePreferences().getFileNamePattern();
        if (fileNamePattern == null || fileNamePattern.trim().isEmpty()) {
            return; // Do nothing if the pattern is empty
        }

        Set<String> patternFields = FileNameParser.parseFields(fileNamePattern);

        System.out.println("Namaste Mummy and Papa!! From LinkedFileAutoRenamer " + patternFields);

        boolean relevantFieldChanged = false;
        if (patternFields.contains("bibtexkey")) {
            changedField.getName();
        }
        if (patternFields.contains(changedField.getName())) {
            relevantFieldChanged = true;
        }

        if (!relevantFieldChanged) {
            return;
        }

        handleEntryChange(entry);
    }

    private void handleEntryChange(BibEntry entry) {
        List<Path> fileDirectories = bibDatabaseContext.getFileDirectories(preferences.getFilePreferences());

        System.out.println("Namaste Mummy and Papa!! Entered handleEntryChange");

        List<LinkedFile> linkedFiles = entry.getFiles();

        for (LinkedFile linkedFile : linkedFiles) {
            Optional<Path> oldFilePathOptional = linkedFile.findIn(fileDirectories);

            if (oldFilePathOptional.isEmpty()) {
                continue;
            }

            Path oldFilePath = oldFilePathOptional.get();

            String extension = FileUtil.getFileExtension(oldFilePath).get();
            if (extension.isEmpty()) {
                LOGGER.warn("File '{}' has no extension. Skipping renaming.", oldFilePath);
                continue; // skip files without an extension
            }

            String newFileName = FileUtil.createFileNameFromPattern(
                    bibDatabaseContext.getDatabase(),
                    entry,
                    preferences.getFilePreferences().getFileNamePattern()
            );

            System.out.println("Namaste Mummy and Papa!! New File name: " + newFileName);

            newFileName = newFileName + "." + extension;

            newFileName = FileNameCleaner.cleanFileName(newFileName);

            if (newFileName.isEmpty()) {
                continue; // Invalid new file name
            }

            // check if file name has changed
            if (oldFilePath.getFileName().toString().equals(newFileName)) {
                continue;
            }

            Path newFilePath = oldFilePath.getParent().resolve(newFileName);

            try {
                Files.move(oldFilePath, newFilePath);

                linkedFile.setLink(newFilePath.toAbsolutePath().toString());

                UiTaskExecutor.runInJavaFXThread(() -> {
                    entry.setFiles(linkedFiles);
                });

                LOGGER.info("Renamed file '{}' to '{}'", oldFilePath, newFilePath);
                System.out.println("Namaste Mummy and Papa!! Renamed file '" + oldFilePath + "' to '" + newFilePath + "'");
            } catch (IOException e) {
                LOGGER.error("Failed to rename file '{}' to '{}'", oldFilePath, newFilePath, e);
                System.out.println("Namaste Mummy and Papa!! Error renaming file '" + oldFilePath + "' to '" + newFilePath + "': " + e.getMessage());
            }
        }
    }

    public void unregisterListener() {
        bibDatabaseContext.getDatabase().unregisterListener(this);
    }
}
