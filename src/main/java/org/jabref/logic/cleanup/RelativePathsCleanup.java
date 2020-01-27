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
            if (isWebUrl(oldFileName)) {
                // let web url untouched
                newFileName = oldFileName;
            }
            else {
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

    /**
     * Checks, if the given file path is a well-formed web url
     *
     * @param filePath file path to check
     * @return <code>true</code>, if the given file path is a web url, <code>false</code> otherwise
     */
    private static boolean isWebUrl(String filePath)
    {
        if (filePath == null) {
            return false;
        }
        String normalizedFilePath = filePath.trim().toLowerCase();
        if (normalizedFilePath.startsWith("http://") || normalizedFilePath.startsWith("https://")) { // INFO: can be extended, if needed
            return true;
        }
        return false;
    }
}
