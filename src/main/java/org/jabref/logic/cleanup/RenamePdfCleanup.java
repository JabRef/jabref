package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenamePdfCleanup implements CleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenamePdfCleanup.class);

    private final BibDatabaseContext databaseContext;
    private final boolean onlyRelativePaths;
    private final String fileNamePattern;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private int unsuccessfulRenames;
    private LinkedFile singleFieldCleanup;

    // FIXME: (S.G.) remove unused constructor argument 'layoutPreferences' later; for now,
    // however, the argument is retained in order not to change the class interface:
    public RenamePdfCleanup(boolean onlyRelativePaths, BibDatabaseContext databaseContext, String fileNamePattern,
                            LayoutFormatterPreferences layoutPreferences,
                            FileDirectoryPreferences fileDirectoryPreferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.onlyRelativePaths = onlyRelativePaths;
        this.fileNamePattern = Objects.requireNonNull(fileNamePattern);
        this.fileDirectoryPreferences = fileDirectoryPreferences;
    }

    public RenamePdfCleanup(boolean onlyRelativePaths, BibDatabaseContext databaseContext, String fileNamePattern,
                            LayoutFormatterPreferences layoutPreferences,
                            FileDirectoryPreferences fileDirectoryPreferences, LinkedFile singleField) {

        this(onlyRelativePaths, databaseContext, fileNamePattern, layoutPreferences,
                fileDirectoryPreferences);
        this.singleFieldCleanup = singleField;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        try {
            return cleanupWithException(entry);
        } catch (IOException e) {
            LOGGER.error("Cleanup failed", e);
            return Collections.emptyList();
        }
    }

    public List<FieldChange> cleanupWithException(BibEntry entry) throws IOException {
        List<LinkedFile> newFileList;
        List<LinkedFile> oldFileList;
        if (singleFieldCleanup != null) {
            oldFileList = Collections.singletonList(singleFieldCleanup);

            newFileList = entry.getFiles().stream().filter(x -> !x.equals(singleFieldCleanup))
                    .collect(Collectors.toList());
        } else {
            newFileList = new ArrayList<>();
            oldFileList = entry.getFiles();
        }

        boolean changed = false;

        for (LinkedFile oldLinkedFile : oldFileList) {
            String realOldFilename = oldLinkedFile.getLink();

            if (StringUtil.isBlank(realOldFilename)) {
                continue; //Skip empty filenames
            }

            if (onlyRelativePaths && Paths.get(realOldFilename).isAbsolute()) {
                newFileList.add(oldLinkedFile);
                continue;
            }

            //old path and old filename
            Optional<Path> expandedOldFile = oldLinkedFile.findIn(databaseContext, fileDirectoryPreferences);

            if ((!expandedOldFile.isPresent()) || (expandedOldFile.get().getParent() == null)) {
                // something went wrong. Just skip this entry
                newFileList.add(oldLinkedFile);
                continue;
            }
            String targetFileName = getTargetFileName(oldLinkedFile, entry);
            Path newPath = expandedOldFile.get().getParent().resolve(targetFileName);

            String expandedOldFilePath = expandedOldFile.get().toString();
            boolean pathsDifferOnlyByCase = newPath.toString().equalsIgnoreCase(expandedOldFilePath)
                    && !newPath.toString().equals(expandedOldFilePath);

            if (Files.exists(newPath) && !pathsDifferOnlyByCase) {
                // we do not overwrite files
                // Since File.exists is sometimes not case-sensitive, the check pathsDifferOnlyByCase ensures that we
                // nonetheless rename files to a new name which just differs by case.
                // TODO: we could check here if the newPath file is linked with the current entry. And if not, we could add a link
                LOGGER.debug("There already exists a file with that name " + newPath.getFileName() + " so I won't rename it");
                newFileList.add(oldLinkedFile);
                continue;
            }

            try {
                if (!Files.exists(newPath)) {
                    Files.createDirectories(newPath);
                }
            } catch (IOException e) {
                LOGGER.error("Could not create necessary target directories for renaming", e);
            }

            boolean renameSuccessful = FileUtil.renameFileWithException(Paths.get(expandedOldFilePath), newPath, true);
            if (renameSuccessful) {
                changed = true;

                // Change the path for this entry
                String description = oldLinkedFile.getDescription();
                String type = oldLinkedFile.getFileType();

                // We use the file directory (if none is set - then bib file) to create relative file links.
                // The .get() is legal without check because the method will always return a value.
                Path settingsDir = databaseContext.getFirstExistingFileDir(fileDirectoryPreferences).get();
                if (settingsDir.getRoot().equals(newPath.getRoot())) {
                    newFileList.add(new LinkedFile(description, settingsDir.relativize(newPath).toString(), type));
                } else {
                    newFileList.add(new LinkedFile(description, newPath.toString(), type));
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
            return change.map(Collections::singletonList).orElseGet(Collections::emptyList);
        }
        return Collections.emptyList();
    }

    public String getTargetFileName(LinkedFile flEntry, BibEntry entry) {
        String realOldFilename = flEntry.getLink();

        String targetFileName = FileUtil.createFileNameFromPattern(
                databaseContext.getDatabase(), entry, fileNamePattern).trim()
                + '.'
                + FileHelper.getFileExtension(realOldFilename).orElse("pdf");

        // Only create valid file names
        return FileUtil.getValidFileName(targetFileName);
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }

    /**
    * Check to see if a file already exists in the target directory.  Search is not case sensitive.
    * @param flEntry
    * @param entry
    * @return First identified path that matches an existing file.  This name can be used in subsequent calls to override the existing file.
    */
    public Optional<Path> findExistingFile(LinkedFile flEntry, BibEntry entry) {
        String targetFileName = getTargetFileName(flEntry, entry);
        // The .get() is legal without check because the method will always return a value.
        Path targetFilePath = flEntry.findIn(databaseContext,
                fileDirectoryPreferences).get().getParent().resolve(targetFileName);
        Path oldFilePath = flEntry.findIn(databaseContext, fileDirectoryPreferences).get();
        //Check if file already exists in directory with different case.
        //This is necessary because other entries may have such a file.
        Optional<Path> matchedByDiffCase = Optional.empty();
        try (Stream<Path> stream = Files.list(oldFilePath.getParent())) {
            matchedByDiffCase = stream
                    .filter(name -> name.toString().equalsIgnoreCase(targetFilePath.toString()))
                    .findFirst();
        } catch (IOException e) {
            LOGGER.error("Could not get the list of files in target directory", e);
        }
        return matchedByDiffCase;
    }
}
