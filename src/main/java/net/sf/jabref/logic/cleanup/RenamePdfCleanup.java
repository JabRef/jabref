package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

public class RenamePdfCleanup implements CleanupJob {

    private final BibDatabaseContext databaseContext;
    private final boolean onlyRelativePaths;
    private final String fileNamePattern;
    private final LayoutFormatterPreferences prefs;
    private final FileDirectoryPreferences fileDirectoryPreferences;

    private int unsuccessfulRenames;


    public RenamePdfCleanup(boolean onlyRelativePaths, BibDatabaseContext databaseContext,
 String fileNamePattern,
            LayoutFormatterPreferences prefs, FileDirectoryPreferences fileDirectoryPreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.onlyRelativePaths = onlyRelativePaths;
        this.fileNamePattern = Objects.requireNonNull(fileNamePattern);
        this.prefs = Objects.requireNonNull(prefs);
        this.fileDirectoryPreferences = fileDirectoryPreferences;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        TypedBibEntry typedEntry = new TypedBibEntry(entry, databaseContext);
        List<ParsedFileField> fileList = typedEntry.getFiles();
        List<ParsedFileField> newFileList = new ArrayList<>();
        boolean changed = false;

        for (ParsedFileField flEntry : fileList) {
            String realOldFilename = flEntry.getLink();

            if (onlyRelativePaths && (new File(realOldFilename).isAbsolute())) {
                newFileList.add(flEntry);
                continue;
            }

            StringBuilder newFilename = new StringBuilder(FileUtil
                    .createFileNameFromPattern(databaseContext.getDatabase(), entry, fileNamePattern, prefs).trim());

            //Add extension to newFilename
            newFilename.append('.').append(FileUtil.getFileExtension(realOldFilename).orElse("pdf"));

            //get new Filename with path
            //Create new Path based on old Path and new filename
            Optional<File> expandedOldFile = FileUtil.expandFilename(realOldFilename,
                    databaseContext.getFileDirectory(fileDirectoryPreferences));
            if ((!expandedOldFile.isPresent()) || (expandedOldFile.get().getParent() == null)) {
                // something went wrong. Just skip this entry
                newFileList.add(flEntry);
                continue;
            }
            String newPath = expandedOldFile.get().getParent().concat(OS.FILE_SEPARATOR).concat(newFilename.toString());

            String expandedOldFilePath = expandedOldFile.get().toString();
            boolean pathsDifferOnlyByCase = newPath.equalsIgnoreCase(expandedOldFilePath)
                    && !newPath.equals(expandedOldFilePath);
            if (new File(newPath).exists() && !pathsDifferOnlyByCase) {
                // we do not overwrite files
                // Since File.exists is sometimes not case-sensitive, the check pathsDifferOnlyByCase ensures that we
                // nonetheless rename files to a new name which just differs by case.
                // TODO: we could check here if the newPath file is linked with the current entry. And if not, we could add a link
                newFileList.add(flEntry);
                continue;
            }

            //do rename
            boolean renameSuccessful = FileUtil.renameFile(expandedOldFilePath, newPath);
            if (renameSuccessful) {
                changed = true;

                //Change the path for this entry
                String description = flEntry.getDescription();
                String type = flEntry.getFileType();

                // we cannot use "newPath" to generate a FileListEntry as newPath is absolute, but we want to keep relative paths whenever possible
                File parent = (new File(realOldFilename)).getParentFile();
                String newFileEntryFileName;
                if ((parent == null) || databaseContext.getFileDirectory(fileDirectoryPreferences)
                        .contains(parent.getAbsolutePath())) {
                    newFileEntryFileName = newFilename.toString();
                } else {
                    newFileEntryFileName = parent.toString().concat(OS.FILE_SEPARATOR).concat(newFilename.toString());
                }
                newFileList.add(new ParsedFileField(description, newFileEntryFileName, type));
            } else {
                unsuccessfulRenames++;
            }
        }

        if (changed) {
            Optional<FieldChange> change = typedEntry.setFiles(newFileList);
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
