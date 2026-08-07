package org.jabref.logic.cleanup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.OptionalUtil;

import org.jspecify.annotations.NonNull;

public class RemoveLinksToNotExistentFiles implements CleanupJob {
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public RemoveLinksToNotExistentFiles(@NonNull BibDatabaseContext databaseContext,
                                         @NonNull FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<LinkedFile> files = entry.getFiles();
        List<LinkedFile> cleanedUpFiles = new ArrayList<>();
        boolean changed = false;
        for (LinkedFile file : files) {
            if (file.isOnlineLink()) {
                cleanedUpFiles.add(file);
            } else {
                Optional<Path> oldFile = file.findIn(databaseContext, filePreferences);

                if (oldFile.isEmpty()) {
                    changed = true;
                } else {
                    cleanedUpFiles.add(file);
                }
            }
        }

        if (changed) {
            Optional<FieldChange> changes = entry.setFiles(cleanedUpFiles);
            return OptionalUtil.toList(changes);
        }

        return List.of();
    }
}
