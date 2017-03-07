package org.jabref.logic.cleanup;

import java.io.File;
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

import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedFileField;
import org.jabref.model.metadata.FileDirectoryPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MoveFilesCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final LayoutFormatterPreferences layoutPrefs;

    private final String fileDirPattern;
    private static final Log LOGGER = LogFactory.getLog(MoveFilesCleanup.class);

    private ParsedFileField singleFileFieldCleanup;

    public MoveFilesCleanup(BibDatabaseContext databaseContext, String fileDirPattern,
            FileDirectoryPreferences fileDirectoryPreferences, LayoutFormatterPreferences layoutPrefs) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.fileDirPattern = Objects.requireNonNull(fileDirPattern);
        this.fileDirectoryPreferences = Objects.requireNonNull(fileDirectoryPreferences);
        this.layoutPrefs = Objects.requireNonNull(layoutPrefs);
    }

    public MoveFilesCleanup(BibDatabaseContext databaseContext, String fileDirPattern,
            FileDirectoryPreferences fileDirectoryPreferences, LayoutFormatterPreferences prefs,
            ParsedFileField field) {

        this(databaseContext, fileDirPattern, fileDirectoryPreferences, prefs);
        this.singleFileFieldCleanup = field;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        Optional<Path> firstExistingFileDir = databaseContext.getFirstExistingFileDir(fileDirectoryPreferences);

        if (!firstExistingFileDir.isPresent()) {
            return Collections.emptyList();
        }

        List<String> paths = databaseContext.getFileDirectories(fileDirectoryPreferences);
        String defaultFileDirectory = firstExistingFileDir.get().toString();
        Optional<Path> targetDirectory = FileUtil.expandFilename(defaultFileDirectory, paths).map(File::toPath);

        if (!targetDirectory.isPresent()) {
            return Collections.emptyList();
        }

        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> fileList;
        List<ParsedFileField> newFileList;

        if (singleFileFieldCleanup != null) {
            fileList = Arrays.asList(singleFileFieldCleanup);
            //Add all other except the current selected file
            newFileList = typedEntry.getFiles().stream().filter(name -> !name.equals(singleFileFieldCleanup))
                    .collect(Collectors.toList());
        } else {
            newFileList = new ArrayList<>();
            fileList = typedEntry.getFiles();
        }

        boolean changed = false;
        for (ParsedFileField fileEntry : fileList) {
            String oldFileName = fileEntry.getLink();

            Optional<File> oldFile = FileUtil.expandFilename(oldFileName, paths);
            if (!oldFile.isPresent() || !oldFile.get().exists()) {
                newFileList.add(fileEntry);
                continue;
            }
            String targetDirName = "";
            if (!fileDirPattern.isEmpty()) {
                targetDirName = FileUtil.createFileNameFromPattern(databaseContext.getDatabase(), entry, fileDirPattern,
                        layoutPrefs);
            }

            Path newTargetFile = targetDirectory.get().resolve(targetDirName).resolve(oldFile.get().getName());
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

            if (FileUtil.renameFile(oldFile.get().toPath(), newTargetFile, true)) {
                changed = true;

                String newEntryFilePath = Paths.get(defaultFileDirectory).relativize(newTargetFile).toString();
                ParsedFileField newFileEntry = fileEntry;
                if (!oldFileName.equals(newTargetFile.toString())) {
                    newFileEntry = new ParsedFileField(fileEntry.getDescription(), newEntryFilePath,
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
