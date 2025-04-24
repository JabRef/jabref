package org.jabref.logic.cleanup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moves the file into the default file directory. Does <em>not</em> rename the file.
 */
public class MoveFilesCleanup implements CleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveFilesCleanup.class);

    private final Supplier<BibDatabaseContext> databaseContext;
    private final FilePreferences filePreferences;
    private final List<JabRefException> ioExceptions;

    public MoveFilesCleanup(Supplier<BibDatabaseContext> databaseContext, FilePreferences filePreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.filePreferences = Objects.requireNonNull(filePreferences);
        this.ioExceptions = new ArrayList<>();
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<LinkedFile> files = entry.getFiles();

        boolean changed = false;
        for (LinkedFile file : files) {
            LinkedFileHandler fileHandler = new LinkedFileHandler(file, entry, databaseContext.get(), filePreferences);
            try {
                changed = fileHandler.moveToDefaultDirectory() || changed;
            } catch (IOException exception) {
                LOGGER.error("Error while moving file {}", file.getLink(), exception);
                ioExceptions.add(new JabRefException(Localization.lang("Could not move file %0. Please close this file and retry.", file.getLink()), exception));
            }
        }

        if (changed) {
            Optional<FieldChange> changes = entry.setFiles(files);
            return OptionalUtil.toList(changes);
        }

        return Collections.emptyList();
    }

    public List<JabRefException> getIoExceptions() {
        return ioExceptions;
    }
}
