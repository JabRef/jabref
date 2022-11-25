package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileHandler.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final BibEntry entry;

    private final LinkedFile fileEntry;

    public LinkedFileHandler(LinkedFile fileEntry,
                             BibEntry entry,
                             BibDatabaseContext databaseContext,
                             FilePreferences filePreferences) {
        this.fileEntry = fileEntry;
        this.entry = entry;
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.filePreferences = Objects.requireNonNull(filePreferences);
    }

    public boolean moveToDefaultDirectory() throws IOException {
        Optional<Path> targetDirectory = databaseContext.getFirstExistingFileDir(filePreferences);
        if (targetDirectory.isEmpty()) {
            return false;
        }

        Optional<Path> oldFile = fileEntry.findIn(databaseContext, filePreferences);
        if (oldFile.isEmpty()) {
            // Could not find file
            return false;
        }

        String targetDirectoryName = "";
        if (!filePreferences.getFileDirectoryPattern().isEmpty()) {
            targetDirectoryName = FileUtil.createDirNameFromPattern(
                    databaseContext.getDatabase(),
                    entry,
                    filePreferences.getFileDirectoryPattern());
        }

        Path targetPath = targetDirectory.get().resolve(targetDirectoryName).resolve(oldFile.get().getFileName());
        if (Files.exists(targetPath)) {
            // We do not overwrite already existing files
            LOGGER.debug("The file {} would have been moved to {}. However, there exists already a file with that name so we do nothing.", oldFile.get(), targetPath);
            return false;
        } else {
            // Make sure sub-directories exist
            Files.createDirectories(targetPath.getParent());
        }

        // Move
        Files.move(oldFile.get(), targetPath);

        // Update path
        fileEntry.setLink(relativize(targetPath));
        return true;
    }

    public boolean renameToSuggestedName() throws IOException {
        return renameToName(getSuggestedFileName(), false);
    }

    public boolean renameToName(String targetFileName, boolean overwriteExistingFile) throws IOException {
        Optional<Path> oldFile = fileEntry.findIn(databaseContext, filePreferences);
        if (oldFile.isEmpty()) {
            return false;
        }

        final Path oldPath = oldFile.get();
        final Path newPath = oldPath.resolveSibling(targetFileName);

        String expandedOldFilePath = oldPath.toString();
        boolean pathsDifferOnlyByCase = newPath.toString().equalsIgnoreCase(expandedOldFilePath)
                && !newPath.toString().equals(expandedOldFilePath);

        // Since Files.exists is sometimes not case-sensitive, the check pathsDifferOnlyByCase ensures that we
        // nonetheless rename files to a new name which just differs by case.
        if (Files.exists(newPath) && !pathsDifferOnlyByCase && !overwriteExistingFile) {
            LOGGER.debug("The file {} would have been moved to {}. However, there exists already a file with that name so we do nothing.", oldPath, newPath);
            return false;
        }

        if (Files.exists(newPath) && !pathsDifferOnlyByCase && overwriteExistingFile) {
            Files.createDirectories(newPath.getParent());
            LOGGER.debug("Overwriting existing file {}", newPath);
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.createDirectories(newPath.getParent());
            Files.move(oldPath, newPath);
        }

        // Update path
        fileEntry.setLink(relativize(newPath));

        return true;
    }

    private String relativize(Path path) {
        List<Path> fileDirectories = databaseContext.getFileDirectories(filePreferences);
        return FileUtil.relativize(path, fileDirectories).toString();
    }

    public String getSuggestedFileName() {
        String oldFileName = fileEntry.getLink();

        String extension = FileHelper.getFileExtension(oldFileName).orElse(fileEntry.getFileType());
        return getSuggestedFileName(extension);
    }

    public String getSuggestedFileName(String extension) {
        String targetFileName = FileUtil.createFileNameFromPattern(databaseContext.getDatabase(), entry, filePreferences.getFileNamePattern()).trim()
                + '.'
                + extension;

        // Only create valid file names
        return FileUtil.getValidFileName(targetFileName);
    }

    /**
     * Check to see if a file already exists in the target directory.  Search is not case sensitive.
     *
     * @return First identified path that matches an existing file.  This name can be used in subsequent calls to
     * override the existing file.
     */
    public Optional<Path> findExistingFile(LinkedFile flEntry, BibEntry entry, String targetFileName) {
        // The .get() is legal without check because the method will always return a value.
        Path targetFilePath = flEntry.findIn(databaseContext, filePreferences)
                                     .get().getParent().resolve(targetFileName);
        Path oldFilePath = flEntry.findIn(databaseContext, filePreferences).get();
        // Check if file already exists in directory with different case.
        // This is necessary because other entries may have such a file.
        Optional<Path> matchedByDiffCase = Optional.empty();
        try (Stream<Path> stream = Files.list(oldFilePath.getParent())) {
            matchedByDiffCase = stream.filter(name -> name.toString().equalsIgnoreCase(targetFilePath.toString()))
                                      .findFirst();
        } catch (IOException e) {
            LOGGER.error("Could not get the list of files in target directory", e);
        }
        return matchedByDiffCase;
    }
}
