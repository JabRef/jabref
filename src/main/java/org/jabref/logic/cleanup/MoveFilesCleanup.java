package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.util.FileHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveFilesCleanup implements CleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveFilesCleanup.class);
    private final BibDatabaseContext databaseContext;
    private final FileDirectoryPreferences fileDirectoryPreferences;

    private final String fileDirPattern;

    private LinkedFile singleFileFieldCleanup;

    // FIXME: remove unused parameter 'layoutPrefs' later S.G.
    public MoveFilesCleanup(BibDatabaseContext databaseContext, String fileDirPattern,
            FileDirectoryPreferences fileDirectoryPreferences, LayoutFormatterPreferences layoutPrefs) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.fileDirPattern = Objects.requireNonNull(fileDirPattern);
        this.fileDirectoryPreferences = Objects.requireNonNull(fileDirectoryPreferences);
    }

    public MoveFilesCleanup(BibDatabaseContext databaseContext, String fileDirPattern,
            FileDirectoryPreferences fileDirectoryPreferences, LayoutFormatterPreferences prefs,
            LinkedFile field) {

        this(databaseContext, fileDirPattern, fileDirectoryPreferences, prefs);
        this.singleFileFieldCleanup = field;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        Optional<Path> firstExistingFileDir = databaseContext.getFirstExistingFileDir(fileDirectoryPreferences);

        if (!firstExistingFileDir.isPresent()) {
            return Collections.emptyList();
        }

        List<Path> paths = databaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences);
        String defaultFileDirectory = firstExistingFileDir.get().toString();
        Optional<Path> targetDirectory = FileHelper.expandFilenameAsPath(defaultFileDirectory, paths);

        if (!targetDirectory.isPresent()) {
            return Collections.emptyList();
        }

        List<LinkedFile> fileList;
        List<LinkedFile> newFileList;

        if (singleFileFieldCleanup != null) {
            fileList = Arrays.asList(singleFileFieldCleanup);
            //Add all other except the current selected file
            newFileList = entry.getFiles().stream().filter(name -> !name.equals(singleFileFieldCleanup))
                    .collect(Collectors.toList());
        } else {
            newFileList = new ArrayList<>();
            fileList = entry.getFiles();
        }

        boolean changed = false;
        for (LinkedFile fileEntry : fileList) {
            String oldFileName = fileEntry.getLink();

            Optional<Path> oldFile = fileEntry.findIn(paths);
            if (!oldFile.isPresent() || !Files.exists(oldFile.get())) {
                newFileList.add(fileEntry);
                continue;
            }
            String targetDirName = "";
            if (!fileDirPattern.isEmpty()) {
                targetDirName = FileUtil.createDirNameFromPattern(databaseContext.getDatabase(), entry, fileDirPattern);
            }

            Path newTargetFile = targetDirectory.get().resolve(targetDirName).resolve(oldFile.get().getFileName());
            if (Files.exists(newTargetFile)) {
                // We do not overwrite already existing files
                newFileList.add(fileEntry);
                continue;
            }

            try {
                if (!Files.exists(newTargetFile)) {
                    Files.createDirectories(newTargetFile);
                }
            } catch (IOException e) {
                LOGGER.error("Could no create necessary target directoires for renaming", e);
            }

            if (FileUtil.renameFile(oldFile.get(), newTargetFile, true)) {
                changed = true;

                String newEntryFilePath = Paths.get(defaultFileDirectory).relativize(newTargetFile).toString();
                LinkedFile newFileEntry = fileEntry;
                if (!oldFileName.equals(newTargetFile.toString())) {
                    newFileEntry = new LinkedFile(fileEntry.getDescription(), newEntryFilePath,
                            fileEntry.getFileType());
                    changed = true;
                }
                newFileList.add(newFileEntry);
            }
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
