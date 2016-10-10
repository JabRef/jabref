package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

public class MoveFilesCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;
    private final FileDirectoryPreferences fileDirectoryPreferences;


    public MoveFilesCleanup(BibDatabaseContext databaseContext, FileDirectoryPreferences fileDirectoryPreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.fileDirectoryPreferences = Objects.requireNonNull(fileDirectoryPreferences);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if(!databaseContext.getMetaData().getDefaultFileDirectory().isPresent()) {
            return Collections.emptyList();
        }

        List<String> paths = databaseContext.getFileDirectory(fileDirectoryPreferences);
        String defaultFileDirectory = databaseContext.getMetaData().getDefaultFileDirectory().get();
        Optional<File> targetDirectory = FileUtil.expandFilename(defaultFileDirectory, paths);
        if(!targetDirectory.isPresent()) {
            return Collections.emptyList();
        }

        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> fileList = typedEntry.getFiles();
        List<ParsedFileField> newFileList = new ArrayList<>();
        boolean changed = false;
        for (ParsedFileField fileEntry : fileList) {
            String oldFileName = fileEntry.getLink();

            Optional<File> oldFile = FileUtil.expandFilename(oldFileName, paths);
            if(!oldFile.isPresent() || !oldFile.get().exists()) {
                newFileList.add(fileEntry);
                continue;
            }

            File targetFile = new File(targetDirectory.get(), oldFile.get().getName());
            if(targetFile.exists()) {
                // We do not overwrite already existing files
                newFileList.add(fileEntry);
                continue;
            }

            oldFile.get().renameTo(targetFile);
            String newFileName = targetFile.getName();

            ParsedFileField newFileEntry = fileEntry;
            if (!oldFileName.equals(newFileName)) {
                newFileEntry = new ParsedFileField(fileEntry.getDescription(), newFileName, fileEntry.getFileType());
                changed = true;
            }
            newFileList.add(newFileEntry);
        }

        if (changed) {
            Optional<FieldChange> change = typedEntry.setFiles(newFileList);
            if(change.isPresent()) {
                return Collections.singletonList(change.get());
            } else {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }

}
