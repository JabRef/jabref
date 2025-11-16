package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class LinkedFileTransferHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileTransferHelper.class);

    /**
     * Adjusts linked files when copying entries from source to target context.
     * Files that are not reachable from the target context will be copied.
     * Files in the target context whose relative paths differ from the source will have their paths adjusted.
     * <p>
     * There is no need to know the source entry, because we are interested in the file paths only.
     *
     * @param filePreferences File preferences for both contexts
     * @param sourceContext   The source database context where files are currently located
     * @param targetContext   The target database context where files should be accessible
     * @param targetEntry     The entry in thet targetContext
     */
    public static void adjustLinkedFilesForTarget(
            FilePreferences filePreferences,
            BibDatabaseContext sourceContext,
            BibDatabaseContext targetContext,
            BibEntry targetEntry
    ) {
        if (!filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()) {
            return;
        }

        LOGGER.debug("Hanndling {}", targetEntry.getKeyAuthorTitleYear());

        boolean fileLinksChanged = false;
        List<LinkedFile> linkedFiles = new ArrayList<>();

        for (LinkedFile linkedFile : targetEntry.getFiles()) {
            LOGGER.debug("Handling file {}", linkedFile);
            if (linkedFile.getLink().isEmpty() || linkedFile.isOnlineLink()) {
                linkedFiles.add(linkedFile);
                continue;
            }

            Path linkedFileAsPath = Path.of(linkedFile.getLink());
            if (linkedFileAsPath.isAbsolute()) {
                // In case the file is an absolute path, there is no need to adjust anything
                linkedFiles.add(linkedFile);
                continue;
            }

            // Check target bibdatabase context offers any directory to store files in
            // Condition works, because absolute paths are already skipped
            Optional<Path> targetPrimaryPathOpt = getPrimaryPath(targetContext, filePreferences);
            if (targetPrimaryPathOpt.isEmpty()) {
                linkedFiles.add(linkedFile);
                continue;
            }
            Path targetPrimaryPath = targetPrimaryPathOpt.get();

            Optional<Path> sourcePathOpt = linkedFile.findIn(sourceContext, filePreferences);
            if (sourcePathOpt.isEmpty()) {
                // In case file does not exist, just keep the broken link
                linkedFiles.add(linkedFile);
                continue;
            }
            Path sourcePath = sourcePathOpt.get();
            assert !linkedFileAsPath.isAbsolute();

            if ((linkedFileAsPath.getParent() != null) // the case with no parent is handled at the next if ("Try to find in other directory")
                    && linkedFile.findIn(targetContext, filePreferences).isPresent()) {
                LOGGER.debug("File is reachable as is");
                // File is reachable as is - no need to copy
                linkedFiles.add(linkedFile);
                continue;
            }

            // Try to find in other directory

            List<Path> directories = targetContext.getFileDirectories(filePreferences);
            Optional<Path> otherPlaceFile = findInSubDirs(linkedFileAsPath.getFileName(), directories);
            if (otherPlaceFile.isPresent()) {
                LOGGER.debug("Found in other place", otherPlaceFile);
                String newLink = FileUtil.relativize(otherPlaceFile.get(), directories).toString();
                LOGGER.debug("Setting new link {}", newLink);
                linkedFile.setLink(newLink);
                fileLinksChanged = true;
                linkedFiles.add(linkedFile);
            }

            linkedFileAsPath = targetPrimaryPath.resolve(linkedFileAsPath);
            try {
                Files.createDirectories(linkedFileAsPath.getParent());
            } catch (IOException e) {
                LOGGER.error("Could not create directory for linked files at {}", linkedFileAsPath, e);
                linkedFiles.add(linkedFile);
                continue;
            }

            if (!Files.exists(linkedFileAsPath)) {
                try {
                    // TODO: Currently, we copy the file and do not move it (when Ctrl+X and then Ctrl+V is pressed)
                    Files.copy(sourcePath, linkedFileAsPath, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException e) {
                    LOGGER.error("Could not copy file from {} to {}", sourcePath, linkedFileAsPath, e);
                    linkedFiles.add(linkedFile);
                    continue;
                }
            }

            /*
            Path relative;
            if (sourcePath.startsWith(targetPrimaryPathOpt.get())) {
                relative = targetPrimaryPathOpt.get().relativize(sourcePath);
            } else {
                relative = Path.of("..").resolve(sourcePath.getFileName());
            }

            if (isReachableFromPrimaryDirectory(relative)) {
                // [impl->req~logic.externalfiles.file-transfer.reachable-no-copy~1]
                fileLinksChanged = isPathAdjusted(linkedFile, relative, linkedFiles, fileLinksChanged);
            } else {
                fileLinksChanged = isFileCopied(sourceContext, targetContext, filePreferences, linkedFile, linkedFiles, fileLinksChanged);
            }
             */
        }
        if (fileLinksChanged) {
            targetEntry.setFiles(linkedFiles);
        }
    }

    private static Optional<Path> findInSubDirs(Path fileName, List<Path> directories) {
        try {
            return directories
                    .stream()
                    .flatMap(Unchecked.function(dir -> Files.walk(dir)))
                    .filter(path -> path.getFileName().equals(fileName))
                    .findFirst();
        } catch (UncheckedIOException ex) {
            LOGGER.warn("Could not search for files {}", ex);
            return Optional.empty();
        }
    }

    /**
     * Gets the primary directory path for the given context.
     * This is a utility method extracted from the original implementation.
     *
     * @param context The database context
     * @param filePreferences File preferences for the context
     * @return Optional containing the primary directory path, or empty if none found
     */
    static Optional<Path> getPrimaryPath(BibDatabaseContext context, FilePreferences filePreferences) {
        return context.getFileDirectories(filePreferences).stream().findFirst();
    }
}
