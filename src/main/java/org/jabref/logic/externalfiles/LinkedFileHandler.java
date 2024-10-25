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

    public boolean renameToSuggestedName() throws IOException {
        return renameToName(getSuggestedFileName(), false);
    }

    public boolean renameToName(String targetFileName, boolean overwriteExistingFile) throws IOException {
        Optional<Path> oldFile = linkedFile.findIn(databaseContext, filePreferences);
        if (oldFile.isEmpty()) {
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

        if (Files.exists(newPath) && !pathsDifferOnlyByCase && overwriteExistingFile) {
            Files.createDirectories(newPath.getParent());
            LOGGER.debug("Overwriting existing file {}", newPath);
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.createDirectories(newPath.getParent());
            Files.move(oldPath, newPath);
        }

        // Update path
        linkedFile.setLink(FileUtil.relativize(newPath, databaseContext, filePreferences).toString());

        return true;
    }

    public String getSuggestedFileName() {
        String oldFileName = linkedFile.getLink();

        String extension = FileUtil.getFileExtension(oldFileName).orElse(linkedFile.getFileType());
        return getSuggestedFileName(extension);
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
        return FileUtil.getValidFileName(targetFileName);
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
}
