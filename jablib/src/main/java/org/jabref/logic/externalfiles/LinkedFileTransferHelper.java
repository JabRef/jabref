package org.jabref.logic.externalfiles;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import java.nio.file.Path;
import java.util.List;

public class LinkedFileTransferHelper {

  public static void adjustLinkedFilesForTarget(
    List<BibEntry> entries,
    BibDatabaseContext sourceContext,
    BibDatabaseContext targetContext,
    FilePreferences filePreferences
  ) {
      for (BibEntry entry : entries) {
        entry.getFiles().forEach(linkedFile -> {
          Path targetFile = linkedFile.findIn(sourceContext.getFileDirectories(filePreferences)).orElse(null);
          if (targetFile != null && isReachableFromPrimaryDirectory(targetFile, targetContext, filePreferences)) {
            System.out.println("File is reachable from target context: " + targetFile);
            linkedFile.setLink(targetFile.toString());
          } else {
            System.out.println("File is NOT reachable from target context: " + targetFile);
          }
        });
      }
  }

  public static boolean isReachableFromPrimaryDirectory(Path resolvedFile, BibDatabaseContext targetContext, FilePreferences filePreferences) {
    List<Path> directories = targetContext.getFileDirectories(filePreferences);
    if (directories.isEmpty()) {
      return false;
    }

    Path primary = directories.getFirst();

    try {
      Path relative = primary.relativize(resolvedFile);
      return !relative.startsWith("..") && !relative.isAbsolute();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
