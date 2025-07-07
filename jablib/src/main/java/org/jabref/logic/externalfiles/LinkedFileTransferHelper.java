package org.jabref.logic.externalfiles;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LinkedFileTransferHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileTransferHelper.class);

  public static void adjustLinkedFilesForTarget(
    BibDatabaseContext targetContext,
    FilePreferences filePreferences
  ) {
      for (BibEntry entry : targetContext.getEntries()) {
        boolean entryChanged = false;
        List<LinkedFile> linkedFiles = new ArrayList<>();
        for (LinkedFile linkedFile : entry.getFiles()) {
          if (linkedFile == null || linkedFile.getLink().isEmpty()) {
            continue;
          }

          Optional<Path> currentOpt = linkedFile.findIn(targetContext, filePreferences);
          if (currentOpt.isEmpty()) {
            continue;
          }

          Path current = currentOpt.get();
          Optional<Path> primaryOpt = getPrimaryPath(targetContext, filePreferences);
          if (primaryOpt.isEmpty()) {
            LOGGER.warn("No primary directory found for current context, cannot adjust linked file: {}", linkedFile);
            linkedFiles.add(linkedFile);
            continue;
          }

          Path primary = primaryOpt.get();

          try {
            Path relative = primary.relativize(current);

            String currentLink = linkedFile.getLink();
            String newLink = relative.toString();

            boolean needsPathUpdate = !currentLink.equals(newLink);
            boolean reachable = isReachableFromPrimaryDirectory(relative);

            if (!reachable) {
              copyFile();
            }

            if (needsPathUpdate && reachable) {
              linkedFile.setLink(newLink);
              entryChanged = true;
            }
            linkedFiles.add(linkedFile);
          } catch (IllegalArgumentException e) {
            LOGGER.warn("Cannot relativize path {} against primary directory {}: {}",
              current, primary, e.getMessage());
            linkedFiles.add(linkedFile);
          }
        }
        if (entryChanged) {
          entry.setFiles(linkedFiles);
        }
      }
  }

  private static void copyFile() {
    throw new UnsupportedOperationException("Copying files is not implemented yet.");
  }

  public static boolean isReachableFromPrimaryDirectory(Path relativePath) {
    try {
      return !relativePath.startsWith("..") && !relativePath.isAbsolute();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static Optional<Path> getPrimaryPath(BibDatabaseContext targetContext, FilePreferences filePreferences) {
    List<Path> directories = targetContext.getFileDirectories(filePreferences);
    if (directories.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(directories.getFirst());
  }
}
