package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileHandler.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;
    private final BibEntry entry;

    private final LinkedFile linkedFile;

    public LinkedFileHandler(LinkedFile linkedFile,
                             BibEntry entry,
                             BibDatabaseContext databaseContext,
                             FilePreferences filePreferences) {
        this.linkedFile = linkedFile;
        this.entry = entry;
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.filePreferences = Objects.requireNonNull(filePreferences);
    }

    public boolean moveToDefaultDirectory() throws IOException {
        return copyOrMoveToDefaultDirectory(true, false);
    }

    /**
     * @return true if the file was copied/moved or the same file exists in the target directory
     */
    public boolean copyOrMoveToDefaultDirectory(boolean shouldMove, boolean shouldRenameToFilenamePattern) throws IOException {
        Optional<Path> databaseFileDirectoryOpt = databaseContext.getFirstExistingFileDir(filePreferences);
        if (databaseFileDirectoryOpt.isEmpty()) {
            LOGGER.warn("No existing file directory found");
            return false;
        }
        Path databaseFileDirectory = databaseFileDirectoryOpt.get();

        Optional<Path> sourcePathOpt = linkedFile.findIn(databaseContext, filePreferences);
        if (sourcePathOpt.isEmpty()) {
            LOGGER.warn("Could not find file {}", linkedFile.getLink());
            return false;
        }
        Path sourcePath = sourcePathOpt.get();

        String targetDirectoryName = "";
        if (!filePreferences.getFileDirectoryPattern().isEmpty()) {
            targetDirectoryName = FileUtil.createDirNameFromPattern(
                    databaseContext.getDatabase(),
                    entry,
                    filePreferences.getFileDirectoryPattern());
        }

        Path targetDirectory = databaseFileDirectory.resolve(targetDirectoryName);
        // Ensure that this directory exists
        Files.createDirectories(targetDirectory);

        GetTargetPathResult getTargetPathResult = null;
        if (shouldRenameToFilenamePattern) {
            getTargetPathResult = getTargetPath(sourcePath, targetDirectory, true);
            if (getTargetPathResult.exists) {
                if (shouldMove && !Files.isSameFile(sourcePath, getTargetPathResult.path)) {
                    Files.delete(sourcePath);
                }
                linkedFile.setLink(FileUtil.relativize(getTargetPathResult.path(), databaseContext, filePreferences).toString());
                return true;
            }
        }
        if (!shouldRenameToFilenamePattern || (getTargetPathResult.renamed && !entry.getFiles().isEmpty())) {
            // Either we do not rename to pattern - or UX feature:
            // UX feature: If user adds a file to the entry and JabRef could only add it when renaming to the suggested pattern,
            //             JabRef should keep the original file name
            getTargetPathResult = getTargetPath(sourcePath, targetDirectory, false);
            if (getTargetPathResult.exists) {
                if (shouldMove && !Files.isSameFile(sourcePath, getTargetPathResult.path)) {
                    Files.delete(sourcePath);
                }
                linkedFile.setLink(FileUtil.relativize(getTargetPathResult.path(), databaseContext, filePreferences).toString());
                return true;
            }
        }

        assert !Files.exists(getTargetPathResult.path);
        if (shouldMove) {
            Files.move(sourcePath, getTargetPathResult.path);
        } else {
            Files.copy(sourcePath, getTargetPathResult.path);
        }
        assert Files.exists(getTargetPathResult.path);

        linkedFile.setLink(FileUtil.relativize(getTargetPathResult.path, databaseContext, filePreferences).toString());
        return true;
    }

    /**
     * If exists: the path already exists and has the same content as the given sourcePath
     *
     * @param renamed The original/suggested filename was adapted to fit it
     */
    private record GetTargetPathResult(boolean exists, boolean renamed, Path path) {
    }

    private GetTargetPathResult getTargetPath(Path sourcePath, Path targetDirectory, boolean useSuggestedName) throws IOException {
        Path suggestedFileName;
        if (useSuggestedName) {
            suggestedFileName = Path.of(getSuggestedFileName(FileUtil.getFileExtension(sourcePath).orElse("")));
        } else {
            suggestedFileName = sourcePath.getFileName();
        }

        Path targetPath = targetDirectory.resolve(suggestedFileName);
        boolean renamed = false;
        if (Files.exists(targetPath)) {
            if (Files.mismatch(sourcePath, targetPath) == -1) {
                // In case of source == target, we pretend, we have success
                LOGGER.debug("The file {} would have been copied/moved to {}. However, there exists already a file with that name so we do nothing.", sourcePath, targetPath);
                return new GetTargetPathResult(true, false, targetPath);
            }
            Integer count = 1;
            boolean exists = false;
            do {
                targetPath = targetDirectory.resolve(sourcePath.getFileName() + " (" + count + ")");
                exists = Files.exists(targetPath);
                if (exists && Files.mismatch(sourcePath, targetPath) == -1) {
                    // In case of source == target, we pretend, we have success
                    LOGGER.debug("The file {} would have been copied/moved to {}. However, there exists already a file with that name so we do nothing.", sourcePath, targetPath);
                    return new GetTargetPathResult(true, true, targetPath);
                }
                count++;
            } while (exists);
            LOGGER.debug("The file {} existed in the target path somehow (but with different content). Chose new name {}.", sourcePath, targetPath);
            renamed = true;
        }
        return new GetTargetPathResult(false, renamed, targetPath);
    }

    public boolean renameToSuggestedName() {
        // Determine old path
        Optional<Path> oldPathOptional = linkedFile.findIn(databaseContext, filePreferences);

        if (oldPathOptional.isEmpty()) {
            LOGGER.warn("Could not find file {}", linkedFile.getLink());
            return false;
        }

        Path oldPath = oldPathOptional.get();

        // Get new name
        String newName = getSuggestedFileName();

        // Generate new Path
        Optional<Path> newPathOptional = getSuggestedFilePath(entry, linkedFile, newName, oldPath);
        if (newPathOptional.isEmpty()) {
            LOGGER.warn("Could not generate new path");
            return false;
        }
        Path newPath = newPathOptional.get();

        try {
            boolean renamed = Files.exists(newPath) || Files.move(oldPath, newPath) != null;
            if (renamed) {
                linkedFile.setLink(FileUtil.relativize(newPath, databaseContext, filePreferences).toString());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error renaming file", e);
            return false;
        }
    }

    /**
     * renameToSuggestedName, but with a different return type.
     */
    public Optional<LinkedFile> renameToSuggestedName2() {
        // Determine old path
        Optional<Path> oldPathOptional = linkedFile.findIn(databaseContext, filePreferences);

        if (oldPathOptional.isEmpty()) {
            LOGGER.warn("Could not find file {}", linkedFile.getLink());
            // Try to analyze file link
            String fileLink = linkedFile.getLink();

            // Check link type
            if (fileLink.startsWith("http://") || fileLink.startsWith("https://")) {
                LOGGER.warn("This is a remote URL, cannot rename");
            } else {
                Path linkPath = Path.of(fileLink);
            }

            return Optional.empty();
        }

        Path oldPath = oldPathOptional.get();
        String oldFileName = oldPath.getFileName().toString();

        // Get new name
        String newName = getSuggestedFileName();

        if (newName.equals(oldFileName)) {
            LOGGER.debug("Old file name and new file name are the same, no need to rename");
            return Optional.of(linkedFile);
        }

        Optional<Path> newPathOptional = getSuggestedFilePath(entry, linkedFile, newName, oldPath);
        if (newPathOptional.isEmpty()) {
            LOGGER.warn("Could not generate new path");
            return Optional.empty();
        }
        Path newPath = newPathOptional.get();

        try {
            if (Files.exists(newPath)) {
                // File already exists, check if content is the same
                if (Files.mismatch(oldPath, newPath) == -1) {
                    // Same content, we can simply update the link and delete the old file
                    Files.delete(oldPath);
                    LinkedFile newLinkedFile = new LinkedFile(linkedFile.getDescription(), newPath.getFileName().toString(), linkedFile.getFileType());
                    return Optional.of(newLinkedFile);
                } else {
                    LOGGER.warn("Target file already exists, but content is different, cannot rename");
                    return Optional.empty();
                }
            }

            // New file does not exist yet, try to rename
            Path result = Files.move(oldPath, newPath);
            if (result != null) {
                LinkedFile newLinkedFile = new LinkedFile(linkedFile.getDescription(), FileUtil.relativize(newPath, databaseContext, filePreferences).toString(), linkedFile.getFileType());
                return Optional.of(newLinkedFile);
            } else {
                LOGGER.warn("Failed to rename file");
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error("Error renaming file", e);
            return Optional.empty();
        }
    }

    public Optional<LinkedFile> rename(BibEntry entry, LinkedFile fileInEntry, Path oldPath, String newName) {
        Optional<Path> newPathOptional = getSuggestedFilePath(entry, fileInEntry, newName, oldPath);
        if (newPathOptional.isEmpty()) {
            LOGGER.warn("Could not generate new path");
            return Optional.empty();
        }

        Path newPath = newPathOptional.get();

        try {
            if (Files.exists(newPath)) {
                // File already exists, check if content is the same
                if (Files.mismatch(oldPath, newPath) == -1) {
                    // Same content, we can simply update the link and delete the old file
                    Files.delete(oldPath);
                    LinkedFile newLinkedFile = new LinkedFile(fileInEntry.getDescription(), FileUtil.relativize(newPath, databaseContext, filePreferences).toString(), fileInEntry.getFileType());
                    return Optional.of(newLinkedFile);
                } else {
                    LOGGER.warn("Target file already exists, but content is different, cannot rename");
                    return Optional.empty();
                }
            }

            // New file does not exist yet, try to rename
            Path result = Files.move(oldPath, newPath);
            if (result != null) {
                LinkedFile newLinkedFile = new LinkedFile(fileInEntry.getDescription(), FileUtil.relativize(newPath, databaseContext, filePreferences).toString(), fileInEntry.getFileType());
                return Optional.of(newLinkedFile);
            } else {
                LOGGER.warn("Failed to rename file");
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error("Error renaming file", e);
            return Optional.empty();
        }
    }

    public boolean renameToName(String targetFileName, boolean overwriteExistingFile) throws IOException {
        Optional<Path> oldFile = linkedFile.findIn(databaseContext, filePreferences);
        if (oldFile.isEmpty()) {
            LOGGER.warn("Could not find original file, cannot rename");
            return false;
        }

        final Path oldPath = oldFile.get();

        Optional<String> oldExtension = FileUtil.getFileExtension(oldPath);
        Optional<String> newExtension = FileUtil.getFileExtension(targetFileName);

        Path newPath;
        if (newExtension.isPresent() || (oldExtension.isEmpty() && newExtension.isEmpty())) {
            newPath = oldPath.resolveSibling(targetFileName);
        } else {
            assert oldExtension.isPresent() && newExtension.isEmpty();
            newPath = oldPath.resolveSibling(targetFileName + "." + oldExtension.get());
        }

        String expandedOldFilePath = oldPath.toString();
        boolean pathsDifferOnlyByCase = newPath.toString().equalsIgnoreCase(expandedOldFilePath)
                && !newPath.toString().equals(expandedOldFilePath);

        // Since Files.exists is sometimes not case-sensitive, the check pathsDifferOnlyByCase ensures that we
        // nonetheless rename files to a new name which just differs by case.
        if (Files.exists(newPath) && !pathsDifferOnlyByCase && !overwriteExistingFile) {
            LOGGER.debug("The file {} would have been moved to {}. However, there exists already a file with that name so we do nothing.", oldPath, newPath);
            return false;
        }

        try {
            if (Files.exists(newPath) && !pathsDifferOnlyByCase && overwriteExistingFile) {
                Files.createDirectories(newPath.getParent());
                LOGGER.debug("Overwriting existing file {}", newPath);
                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.createDirectories(newPath.getParent());
                Files.move(oldPath, newPath);
            }

            // Update path
            String oldLink = linkedFile.getLink();
            String newLink = FileUtil.relativize(newPath, databaseContext, filePreferences).toString();
            linkedFile.setLink(newLink);

            return true;
        } catch (IOException e) {
            LOGGER.error("Error moving file", e);
            throw e;
        }
    }

    public String getSuggestedFileName() {
        String oldFileName = linkedFile.getLink();

        String extension = FileUtil.getFileExtension(oldFileName).orElse(linkedFile.getFileType());

        String suggestedName = getSuggestedFileName(extension);
        return suggestedName;
    }

    /**
     * @param extension The extension of the file. If empty, no extension is added.
     * @return A filename based on the pattern specified in the preferences and valid for the file system.
     */
    public String getSuggestedFileName(String extension) {
        String targetFileName = FileUtil.createFileNameFromPattern(databaseContext.getDatabase(), entry, filePreferences.getFileNamePattern()).trim();
        if (!extension.isEmpty()) {
            targetFileName = targetFileName + '.' + extension;
        }
        String validFileName = FileUtil.getValidFileName(targetFileName);
        return validFileName;
    }

    /**
     * Check to see if a file already exists in the target directory.  Search is not case sensitive.
     *
     * @return First identified path that matches an existing file.  This name can be used in subsequent calls to
     * override the existing file.
     */
    public Optional<Path> findExistingFile(LinkedFile linkedFile, BibEntry entry, String targetFileName) {
        // The .get() is legal without check because the method will always return a value.
        Path targetFilePath = linkedFile.findIn(databaseContext, filePreferences)
                                     .get().getParent().resolve(targetFileName);
        Path oldFilePath = linkedFile.findIn(databaseContext, filePreferences).get();
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

    public LinkedFile refreshFileLink() {
        // Calculate the correct relative path based on the current file path and database context
        Optional<Path> filePath = linkedFile.findIn(databaseContext, filePreferences);
        if (filePath.isPresent()) {
            String relativeLink = FileUtil.relativize(filePath.get(), databaseContext, filePreferences).toString();
            return new LinkedFile(
                    linkedFile.getDescription(),
                    relativeLink,
                    linkedFile.getFileType()
            );
        } else {
            // If the file cannot be found, return the original link
            return linkedFile;
        }
    }

    private Optional<Path> getSuggestedFilePath(BibEntry entry, LinkedFile fileInEntry, String newName, Path oldPath) {
        try {
            // Although there is a newName parameter, we need a proper path object. Since we need to construct this path
            // relative to the old path, we need to find the position of the old file's folder.
            String oldFilePath = oldPath.toString();

            // Get directory of old file
            Path directory = oldPath.getParent();

            if (directory == null) {
                return Optional.empty();
            }

            // Try to apply the new file name
            Path newPath = directory.resolve(newName);

            // First attempt: Return the new file name if it does not exist yet
            if (!Files.exists(newPath)) {
                return Optional.of(newPath);
            }

            // If the destination already exists, we check whether its content is the same as the source
            long mismatch = Files.mismatch(oldPath, newPath);
            if (mismatch == -1) {
                // No content difference
                return Optional.of(newPath);
            }

            // Rename using "(number)" behind the name
            int counter = 1;
            int dotPosition = newName.lastIndexOf('.');
            String fileNameWithoutExtension = dotPosition < 0 ? newName : newName.substring(0, dotPosition);
            String extension = dotPosition < 0 ? "" : newName.substring(dotPosition);

            while (Files.exists(newPath)) {
                String fileName = String.format("%s (%d)%s", fileNameWithoutExtension, counter, extension);
                newPath = directory.resolve(fileName);
                counter++;
            }

            return Optional.of(newPath);
        } catch (Exception e) {
            LOGGER.error("Error generating suggested file path", e);
            return Optional.empty();
        }
    }
}
