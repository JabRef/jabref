package net.sf.jabref.logic.cleanup;

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

public class RenamePdfCleanup implements CleanupJob {

    private static final Log LOGGER = LogFactory.getLog(RenamePdfCleanup.class);

    private final BibDatabaseContext databaseContext;
    private final boolean onlyRelativePaths;
    private final String fileNamePattern;
    private final String fileDirPattern;
    private final LayoutFormatterPreferences layoutPrefs;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private int unsuccessfulRenames;
    private ParsedFileField singleFieldCleanup;

    public RenamePdfCleanup(boolean onlyRelativePaths, BibDatabaseContext databaseContext, String fileNamePattern,
            String fileDirPattern, LayoutFormatterPreferences layoutPrefs,
            FileDirectoryPreferences fileDirectoryPreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.onlyRelativePaths = onlyRelativePaths;
        this.fileNamePattern = Objects.requireNonNull(fileNamePattern);
        this.fileDirPattern = Objects.requireNonNull(fileDirPattern);
        this.layoutPrefs = Objects.requireNonNull(layoutPrefs);
        this.fileDirectoryPreferences = fileDirectoryPreferences;
    }

    public RenamePdfCleanup(boolean onlyRelativePaths, BibDatabaseContext databaseContext, String fileNamePattern,
            String fileDirPattern, LayoutFormatterPreferences prefs,
            FileDirectoryPreferences fileDirectoryPreferences, ParsedFileField singleField) {

        this(onlyRelativePaths, databaseContext, fileNamePattern, fileDirPattern, prefs,
                fileDirectoryPreferences);
        this.singleFieldCleanup = singleField;

    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> newFileList;
        List<ParsedFileField> fileList;
        if (singleFieldCleanup != null) {
            fileList = Arrays.asList(singleFieldCleanup);

            newFileList = typedEntry.getFiles().stream().filter(x -> !x.equals(singleFieldCleanup))
                    .collect(Collectors.toList());
        } else {
            newFileList = new ArrayList<>();
            fileList = typedEntry.getFiles();
        }

        boolean changed = false;

        for (ParsedFileField flEntry : fileList) {
            String realOldFilename = flEntry.getLink();

            if (onlyRelativePaths && (new File(realOldFilename).isAbsolute())) {
                newFileList.add(flEntry);
                continue;
            }

            StringBuilder targetFileName = new StringBuilder(FileUtil
                    .createFileNameFromPattern(databaseContext.getDatabase(), entry, fileNamePattern, layoutPrefs)
                    .trim());

            String targetDirName = "";
            if (!fileDirPattern.isEmpty()) {
                targetDirName = FileUtil.createFileNameFromPattern(databaseContext.getDatabase(), entry, fileDirPattern,
                        layoutPrefs);
            }

            //Add extension to newFilename
            targetFileName.append('.').append(FileUtil.getFileExtension(realOldFilename).orElse("pdf"));

            //old path and old filename
            Optional<Path> expandedOldFile = FileUtil.expandFilename(realOldFilename,
                    databaseContext.getFileDirectories(fileDirectoryPreferences)).map(File::toPath);

            if ((!expandedOldFile.isPresent()) || (expandedOldFile.get().getParent() == null)) {
                // something went wrong. Just skip this entry
                newFileList.add(flEntry);
                continue;
            }

            Path newPath = expandedOldFile.get().getParent().resolve(targetDirName).resolve(targetFileName.toString());

            String expandedOldFilePath = expandedOldFile.get().toString();
            boolean pathsDifferOnlyByCase = newPath.toString().equalsIgnoreCase(expandedOldFilePath)
                    && !newPath.toString().equals(expandedOldFilePath);

            if (Files.exists(newPath) && !pathsDifferOnlyByCase) {
                // we do not overwrite files
                // Since File.exists is sometimes not case-sensitive, the check pathsDifferOnlyByCase ensures that we
                // nonetheless rename files to a new name which just differs by case.
                // TODO: we could check here if the newPath file is linked with the current entry. And if not, we could add a link
                newFileList.add(flEntry);
                continue;
            }

            try {
                if (!Files.exists(newPath)) {
                    Files.createDirectories(newPath);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGGER.error("Could no create necessary target directoires for renaming", e);
            }
            //do rename
            boolean renameSuccessful = FileUtil.renameFile(Paths.get(expandedOldFilePath), newPath, true);
            if (renameSuccessful) {
                changed = true;

                //Change the path for this entry
                String description = flEntry.getDescription();
                String type = flEntry.getFileType();

                //We use the file directory (if none is set - then bib file) to create relative file links
                Optional<Path> settingsDir = databaseContext.getFirstExistingFileDir(fileDirectoryPreferences);
                if (settingsDir.isPresent()) {

                    Path parent = settingsDir.get();
                    String newFileEntryFileName;
                    if (parent == null) {
                        newFileEntryFileName = targetFileName.toString();
                    } else {
                        newFileEntryFileName = parent.relativize(newPath).toString();

                    }
                    newFileList.add(new ParsedFileField(description, newFileEntryFileName, type));
                }
            } else {
                unsuccessfulRenames++;
            }
        }
        if (changed) {
            Optional<FieldChange> change = entry.setFiles(newFileList);
            //we put an undo of the field content here
            //the file is not being renamed back, which leads to inconsistencies
            //if we put a null undo object here, the change by "doMakePathsRelative" would overwrite the field value nevertheless.
            if (change.isPresent()) {
                return Collections.singletonList(change.get());
            } else {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }
}
