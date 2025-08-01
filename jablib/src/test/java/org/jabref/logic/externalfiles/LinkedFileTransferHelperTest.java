package org.jabref.logic.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFileTransferHelperTest {
  private BibDatabaseContext sourceContext;
  private BibDatabaseContext targetContext;
  private Path sourceDir;
  private Path targetDir;
  private Path testFile;
  private BibEntry sourceEntry;
  private BibEntry targetEntry;
  private FilePreferences filePreferences = mock(FilePreferences.class);

  @Nested
  class WhenFileIsReachable {

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
      sourceDir = tempDir.resolve("source/target");
      targetDir = tempDir.resolve("source");

      when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);

      Files.createDirectories(sourceDir);
      Files.createDirectories(targetDir);

      testFile = sourceDir.resolve("sourcefiles/test.pdf");
      Files.createDirectories(testFile.getParent());
      Files.createFile(testFile);

      sourceContext = new BibDatabaseContext(new BibDatabase());
      sourceContext.setDatabasePath(sourceDir.resolve("personal.bib"));
      targetContext = new BibDatabaseContext(new BibDatabase());
      targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

      sourceEntry = new BibEntry();
      LinkedFile linkedFile = new LinkedFile("Test", "sourcefiles/test.pdf", "PDF");

      sourceEntry.withFiles(List.of(linkedFile));
      targetEntry = new BibEntry(sourceEntry);
      targetEntry.withFiles(List.of(linkedFile));

      sourceContext.getDatabase().insertEntry(sourceEntry);
      targetContext.getDatabase().insertEntry(targetEntry);
    }

    @Test
    void pathDiffers_ShouldAdjustPath() {
      var returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
        filePreferences);

      assertEquals(1, returnedEntries.size());
      assertEquals("sourcefiles/test.pdf", sourceContext.getEntries().getFirst().getFiles().getFirst().getLink());
      assertEquals("target/sourcefiles/test.pdf",
        targetContext.getEntries().getFirst().getFiles().getFirst().getLink());
      Path expectedFile = targetDir.resolve("test.pdf");
      assertFalse(Files.exists(expectedFile));
    }
  }

  @Nested
  class WhenFileIsNotReachable {

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
      sourceDir = tempDir.resolve("target/targetfiles");
      targetDir = tempDir.resolve("source/sourcefiles");

      when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);

      Files.createDirectories(sourceDir);
      Files.createDirectories(targetDir);

      testFile = sourceDir.resolve("test.pdf");
      Files.createDirectories(testFile.getParent());
      Files.createFile(testFile);

      sourceContext = new BibDatabaseContext(new BibDatabase());
      sourceContext.setDatabasePath(sourceDir.resolve("personal.bib"));
      targetContext = new BibDatabaseContext(new BibDatabase());
      targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

      sourceEntry = new BibEntry();
      LinkedFile linkedFile = new LinkedFile("Test", "test.pdf", "PDF");

      sourceEntry.withFiles(List.of(linkedFile));
      targetEntry = new BibEntry(sourceEntry);
      targetEntry.withFiles(List.of(linkedFile));

      sourceContext.getDatabase().insertEntry(sourceEntry);
      targetContext.getDatabase().insertEntry(targetEntry);
    }

    @Test
    void fileNotReachable_ShouldCopyFile() {
      var returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
        filePreferences);

      assertEquals(1, returnedEntries.size());
      assertEquals("test.pdf", sourceContext.getEntries().getFirst().getFiles().getFirst().getLink());
      assertEquals("test.pdf",
        targetContext.getEntries().getFirst().getFiles().getFirst().getLink());
      Path expectedFile = targetDir.resolve("test.pdf");
      assertTrue(Files.exists(expectedFile));
    }
  }

  @Nested
  class WhenFileIsNotReachableAndPathsDiffer {

    @BeforeEach
    void setup(@TempDir Path tempDir) throws Exception {
      sourceDir = tempDir.resolve("source");
      targetDir = tempDir.resolve("target/targetfiles");

      when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);

      Files.createDirectories(sourceDir);
      Files.createDirectories(targetDir);

      testFile = sourceDir.resolve("sourcefiles/test.pdf");
      Files.createDirectories(testFile.getParent());
      Files.createFile(testFile);

      sourceContext = new BibDatabaseContext(new BibDatabase());
      sourceContext.setDatabasePath(sourceDir.resolve("personal.bib"));
      targetContext = new BibDatabaseContext(new BibDatabase());
      targetContext.setDatabasePath(targetDir.resolve("papers.bib"));

      sourceEntry = new BibEntry();
      LinkedFile linkedFile = new LinkedFile("Test", "sourcefiles/test.pdf", "PDF");

      sourceEntry.withFiles(List.of(linkedFile));
      targetEntry = new BibEntry(sourceEntry);
      targetEntry.withFiles(List.of(linkedFile));

      sourceContext.getDatabase().insertEntry(sourceEntry);
      targetContext.getDatabase().insertEntry(targetEntry);
    }

    @Test
    void fileNotReachableAndPathsDiffer_ShouldCopyFileAndCreateDirectory() {
      var returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
        filePreferences);

      assertEquals(1, returnedEntries.size());
      assertEquals("sourcefiles/test.pdf", sourceContext.getEntries().getFirst().getFiles().getFirst().getLink());
      assertEquals("sourcefiles/test.pdf",
        targetContext.getEntries().getFirst().getFiles().getFirst().getLink());
      Path expectedFile = targetDir.resolve("sourcefiles/test.pdf");
      assertTrue(Files.exists(expectedFile));
    }
  }
}
