package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MoveFilesCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final LayoutFormatterPreferences prefs;

    private final String fileDirPattern;
    private static final Log LOGGER = LogFactory.getLog(MoveFilesCleanup.class);

    public MoveFilesCleanup(BibDatabaseContext databaseContext, String fileDirPattern,
            FileDirectoryPreferences fileDirectoryPreferences, LayoutFormatterPreferences prefs) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.fileDirPattern = Objects.requireNonNull(fileDirPattern);
        this.fileDirectoryPreferences = Objects.requireNonNull(fileDirectoryPreferences);
        this.prefs = Objects.requireNonNull(prefs);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if (!databaseContext.getMetaData().getDefaultFileDirectory().isPresent()) {
            return Collections.emptyList();
        }

        List<String> paths = databaseContext.getFileDirectories(fileDirectoryPreferences);
        String defaultFileDirectory = databaseContext.getMetaData().getDefaultFileDirectory().get();
        Optional<File> targetDirectory = FileUtil.expandFilename(defaultFileDirectory, paths);

        if (!targetDirectory.isPresent()) {
            return Collections.emptyList();
        }

        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> fileList = typedEntry.getFiles();
        List<ParsedFileField> newFileList = new ArrayList<>();

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
                        prefs);
            }

            Path newTargetFile = targetDirectory.get().toPath().resolve(targetDirName).resolve(oldFile.get().getName());
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
