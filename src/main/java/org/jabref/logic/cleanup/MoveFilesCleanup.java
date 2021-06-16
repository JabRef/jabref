package org.jabref.logic.cleanup;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveFilesCleanup implements CleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveFilesCleanup.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public MoveFilesCleanup(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.filePreferences = Objects.requireNonNull(filePreferences);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<LinkedFile> files = entry.getFiles();

        boolean changed = false;
        for (LinkedFile file : files) {
            LinkedFileHandler fileHandler = new LinkedFileHandler(file, entry, databaseContext, filePreferences);
            try {
                boolean fileChanged = fileHandler.moveToDefaultDirectory();
                if (fileChanged) {
                    changed = true;
                }
            } catch (IOException exception) {
                LOGGER.error("Error while moving file {}", file.getLink(), exception);
            }
        }

        if (changed) {
            Optional<FieldChange> changes = entry.setFiles(files);
            return OptionalUtil.toList(changes);
        }

        return Collections.emptyList();
    }
}
