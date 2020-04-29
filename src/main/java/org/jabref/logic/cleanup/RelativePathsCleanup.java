package org.jabref.logic.cleanup;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FilePreferences;

public class RelativePathsCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public RelativePathsCleanup(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.filePreferences = Objects.requireNonNull(filePreferences);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<LinkedFile> fileList = entry.getFiles();
        List<LinkedFile> newFileList = new ArrayList<>();
        boolean changed = false;

        for (LinkedFile fileEntry : fileList) {
            String oldFileName = fileEntry.getLink();
            String newFileName = null;
            if (fileEntry.isOnlineLink()) {
                // keep online link untouched
                newFileName = oldFileName;
            } else {
                // only try to transform local file path to relative one
                newFileName = FileUtil
                        .relativize(Paths.get(oldFileName), databaseContext.getFileDirectoriesAsPaths(filePreferences))
                        .toString();
            }
            LinkedFile newFileEntry = fileEntry;
            if (!oldFileName.equals(newFileName)) {
                newFileEntry = new LinkedFile(fileEntry.getDescription(), newFileName, fileEntry.getFileType());
                changed = true;
            }
            newFileList.add(newFileEntry);
        }

        if (changed) {
            Optional<FieldChange> change = entry.setFiles(newFileList);
            if (change.isPresent()) {
                return Collections.singletonList(change.get());
            } else {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }
}
