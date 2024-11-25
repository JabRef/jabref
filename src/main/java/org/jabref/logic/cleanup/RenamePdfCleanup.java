package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenamePdfCleanup implements CleanupJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenamePdfCleanup.class);

    private final Supplier<BibDatabaseContext> databaseContext;
    private final boolean onlyRelativePaths;
    private final FilePreferences filePreferences;

    public RenamePdfCleanup(boolean onlyRelativePaths, Supplier<BibDatabaseContext> databaseContext, FilePreferences filePreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.onlyRelativePaths = onlyRelativePaths;
        this.filePreferences = filePreferences;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<LinkedFile> files = entry.getFiles();

        boolean changed = false;
        for (LinkedFile file : files) {
            if (onlyRelativePaths && Path.of(file.getLink()).isAbsolute()) {
                continue;
            }

            LinkedFileHandler fileHandler = new LinkedFileHandler(file, entry, databaseContext.get(), filePreferences);
            try {
                boolean changedFile = fileHandler.renameToSuggestedName();
                if (changedFile) {
                    changed = true;
                }
            } catch (IOException exception) {
                LOGGER.error("Error while renaming file {}", file.getLink(), exception);
            }
        }

        if (changed) {
            Optional<FieldChange> changes = entry.setFiles(files);
            return OptionalUtil.toList(changes);
        }

        return Collections.emptyList();
    }
}
