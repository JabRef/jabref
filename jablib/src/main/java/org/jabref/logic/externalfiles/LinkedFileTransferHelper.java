package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFileTransferHelper {

    private record FileCopyContext(
            BibDatabaseContext sourceContext,
            BibDatabaseContext targetContext,
            FilePreferences filePreferences
    ) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileTransferHelper.class);

    /**
     * Adjusts linked files when copying entries from source to target context.
     * Files that are not reachable from the target context will be copied.
     * Files in the target context whose relative paths differ from the source will have their paths adjusted
     *
     * @param sourceContext The source database context where files are currently located
     * @param targetContext The target database context where files should be accessible
     * @param filePreferences File preferences for both contexts
     *
     * @return set of modified entries. Used for testing.
     */
    public static Set<BibEntry> adjustLinkedFilesForTarget(
            BibDatabaseContext sourceContext,
            BibDatabaseContext targetContext,
            FilePreferences filePreferences
    ) {
        if (!filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()) {
            return Set.of();
        }

        Set<BibEntry> modifiedEntries = new HashSet<>();

        FileCopyContext context = new FileCopyContext(sourceContext, targetContext, filePreferences);

        for (BibEntry entry : targetContext.getEntries()) {
            boolean entryChanged = false;
            List<LinkedFile> linkedFiles = new ArrayList<>();

            for (LinkedFile linkedFile : entry.getFiles()) {
                if (linkedFile.getLink().isEmpty() || linkedFile.isOnlineLink()) {
                    linkedFiles.add(linkedFile);
                    continue;
                }

                if (Path.of(linkedFile.getLink()).isAbsolute()) {
                    // In case the file is an absolute path, there is no need to adjust anything
                    linkedFiles.add(linkedFile);
                    continue;
                }

                Optional<Path> sourcePathOpt = linkedFile.findIn(sourceContext, filePreferences);
                if (sourcePathOpt.isEmpty()) {
                    // In case file does not exist, just keep the broken link
                    linkedFiles.add(linkedFile);
                    continue;
                }

                // Check target bibdatabase context offers any directory to store files in
                Optional<Path> targetPrimaryOpt = getPrimaryPath(targetContext, filePreferences);
                if (targetPrimaryOpt.isEmpty()) {
                    linkedFiles.add(linkedFile);
                    continue;
                }

                Path relative;
                if (sourcePathOpt.get().startsWith(targetPrimaryOpt.get())) {
                    relative = targetPrimaryOpt.get().relativize(sourcePathOpt.get());
                } else {
                    relative = Path.of("..").resolve(sourcePathOpt.get().getFileName());
                }

                if (isReachableFromPrimaryDirectory(relative)) {
                    // [impl->req~logic.externalfiles.file-transfer.reachable-no-copy~1]
                    entryChanged = isPathAdjusted(linkedFile, relative, linkedFiles, entryChanged);
                } else {
                    entryChanged = isFileCopied(context, linkedFile, linkedFiles, entryChanged);
                }
            }
            if (entryChanged) {
                entry.setFiles(linkedFiles);
                modifiedEntries.add(entry);
            }
        }
        return modifiedEntries;
    }

    /**
     * Gets the primary directory path for the given context.
     * This is a utility method extracted from the original implementation.
     *
     * @param context The database context
     * @param filePreferences File preferences for the context
     * @return Optional containing the primary directory path, or empty if none found
     */
    public static Optional<Path> getPrimaryPath(BibDatabaseContext context, FilePreferences filePreferences) {
        List<Path> directories = context.getFileDirectories(filePreferences);
        if (directories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(directories.getFirst());
    }

    /**
     * Determines if the given relative path is reachable from the primary directory.
     * A path is considered reachable if it does not start with ".." (i.e., does not traverse up the directory tree)
     * and is not absolute.
     *
     * @param relativePath the path to check, relative to the primary directory
     * @return true if the path is reachable from the primary directory, false otherwise
     */
    public static boolean isReachableFromPrimaryDirectory(Path relativePath) {
        try {
            return !relativePath.startsWith("..") && !relativePath.isAbsolute();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isPathAdjusted(LinkedFile linkedFile, Path relative, List<LinkedFile> linkedFiles, boolean entryChanged) {
        boolean pathUpdated = adjustPathForReachableFile(
                linkedFile, relative
        );
        if (pathUpdated) {
            entryChanged = true;
        }
        linkedFiles.add(linkedFile);
        return entryChanged;
    }

    private static boolean isFileCopied(FileCopyContext context, LinkedFile linkedFile, List<LinkedFile> linkedFiles, boolean entryChanged) {
        boolean fileCopied = copyFileToTargetContext(
                linkedFile, context
        );
        if (fileCopied) {
            Optional<Path> newPath = linkedFile.findIn(context.targetContext(), context.filePreferences());
            newPath.ifPresent(path -> linkedFile.setLink(
                    FileUtil.relativize(path, context.targetContext(), context.filePreferences()).toString())
            );
            entryChanged = true;
        }
        linkedFiles.add(linkedFile);
        return entryChanged;
    }

    /**
     * Adjusts the path of a file that is already reachable from the target context.
     *
     * @return true if the path was updated, false otherwise
     */
    private static boolean adjustPathForReachableFile(
            @NonNull LinkedFile linkedFile,
            @NonNull Path relativePath
    ) {
        // [impl->req~logic.externalfiles.file-transfer.reachable-no-copy~1]
        String newLink = relativePath.toString();
        String currentLink = linkedFile.getLink();
        if (!currentLink.equals(newLink)) {
            linkedFile.setLink(newLink);
            LOGGER.debug("Adjusted path for reachable file: {} -> {}", currentLink, newLink);
            return true;
        }
        return false;
    }

    /**
     * Copies a file linked in a `LinkedFile` from the source context to the target context.
     * Locates the source file using the source context and file preferences, then copies it to the
     * corresponding path in the target context's primary directory, preserving the relative link.
     *
     * @return true if the file was successfully copied, false otherwise
     */
    private static boolean copyFileToTargetContext(
            LinkedFile linkedFile,
            FileCopyContext context
    ) {
        // [impl->req~logic.externalfiles.file-transfer.not-reachable-same-path~1]
        // [impl->req~logic.externalfiles.file-transfer.not-reachable-different-path~1]
        Optional<Path> sourcePathOpt = linkedFile.findIn(context.sourceContext(), context.filePreferences());
        if (sourcePathOpt.isEmpty()) {
            LOGGER.warn("Could not find source file {} to copy", linkedFile.getLink());
            return false;
        }

        Optional<Path> targetDirOpt = getPrimaryPath(context.targetContext(), context.filePreferences());
        if (targetDirOpt.isEmpty()) {
            LOGGER.warn("Could not find any target directory", linkedFile.getLink());
            return false;
        }

        Path sourcePath = sourcePathOpt.get();
        Path relativeLinkPath = Path.of(linkedFile.getLink());
        Path fullTargetPath = targetDirOpt.get().resolve(relativeLinkPath);

        try {
            // [impl->req~logic.externalfiles.file-transfer.not-reachable-different-path~1]
            Files.createDirectories(fullTargetPath.getParent());
            Files.copy(sourcePath, fullTargetPath); // no overwrite
            LOGGER.info("Copied file from {} to {}", sourcePath, fullTargetPath);
            return true;
        } catch (FileAlreadyExistsException e) {
            LOGGER.warn("Target file {} already exists – not overwriting", fullTargetPath);
            return false;
        } catch (IOException e) {
            LOGGER.error("Failed to copy file from {} to {}: {}", sourcePath, fullTargetPath, e.getMessage(), e);
            return false;
        }
    }
}
