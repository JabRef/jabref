package org.jabref.logic.externalfiles;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFileTransferHelperTest {

  @Nested
  class IsReachableFromPrimaryDirectoryTest {

    FilePreferences prefs = mock(FilePreferences.class);
    BibDatabaseContext targetContext = mock(BibDatabaseContext.class);

    @Test
    void case1_reachableWithinPrimaryDirectory() {
      Path targetPrimaryDir = Path.of("/project");
      Path file = Path.of("/project/bibs/personalfiles/file.pdf");

      when(targetContext.getFileDirectories(prefs)).thenReturn(List.of(targetPrimaryDir));

      assertTrue(LinkedFileTransferHelper.isReachableFromPrimaryDirectory(file, targetContext, prefs));
    }

    @Test
    void case2_sameRelativePathButDifferentBaseDirectory_shouldNotBeReachable() {
      Path targetPrimaryDir = Path.of("/project/parentfiles");
      Path file = Path.of("/project/bibs/personalfiles/file.pdf");

      when(targetContext.getFileDirectories(prefs)).thenReturn(List.of(targetPrimaryDir));

      assertFalse(LinkedFileTransferHelper.isReachableFromPrimaryDirectory(file, targetContext, prefs));
    }

    @Test
    void case3_differentRelativePathAndDifferentBaseDirectory_shouldNotBeReachable() {
      Path targetPrimaryDir = Path.of("/project/parentfiles");
      Path file = Path.of("/project/bibs/personalfiles/file.pdf");

      when(targetContext.getFileDirectories(prefs)).thenReturn(List.of(targetPrimaryDir));

      assertFalse(LinkedFileTransferHelper.isReachableFromPrimaryDirectory(file, targetContext, prefs));
    }
  }
}
