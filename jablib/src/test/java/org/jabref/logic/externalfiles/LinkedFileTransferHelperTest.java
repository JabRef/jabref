package org.jabref.logic.externalfiles;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
  @DisplayName("When the linked file is reachable in the new context")
  class WhenFileIsReachable {

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
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

      sourceEntry.setFiles(List.of(linkedFile));
      targetEntry = (BibEntry) sourceEntry.clone();
      targetEntry.setFiles(List.of(linkedFile));

      sourceContext.getDatabase().insertEntry(sourceEntry);
      targetContext.getDatabase().insertEntry(targetEntry);
    }

    @Test
    void pathDiffers_ShouldAdjustPath(@TempDir Path tempDir) throws IOException {
      var returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
        filePreferences);

      assertEquals(1, returnedEntries.size());
      assertEquals("sourcefiles/test.pdf", sourceContext.getEntries().getFirst().getFiles().getFirst().getLink());
      assertEquals("target/sourcefiles/test.pdf",
        targetContext.getEntries().getFirst().getFiles().getFirst().getLink());
      Path expectedFile = targetDir.resolve("test.pdf");
      assertFalse(Files.exists(expectedFile), "File should not have been copied to target directory");
    }
  }

  @Nested
  @DisplayName("When the linked file is NOT reachable, but the paths do NOT differ")
  class WhenFileIsNotReachable {

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
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

      sourceEntry.setFiles(List.of(linkedFile));
      targetEntry = (BibEntry) sourceEntry.clone();
      targetEntry.setFiles(List.of(linkedFile));

      sourceContext.getDatabase().insertEntry(sourceEntry);
      targetContext.getDatabase().insertEntry(targetEntry);
    }

    @Test
    void fileNotReachable_ShouldCopyFile(@TempDir Path tempDir) throws IOException {
      var returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
        filePreferences);

      assertEquals(1, returnedEntries.size());
      assertEquals("test.pdf", sourceContext.getEntries().getFirst().getFiles().getFirst().getLink());
      assertEquals("test.pdf",
        targetContext.getEntries().getFirst().getFiles().getFirst().getLink());
      Path expectedFile = targetDir.resolve("test.pdf");
      assertTrue(Files.exists(expectedFile), "File should have been copied to target directory");
    }
  }

  @Nested
  @DisplayName("When the linked file is NOT reachable, and a nested structure needs to be created")
  class WhenFileIsNotReachableAndPathsDiffer {

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
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

      sourceEntry.setFiles(List.of(linkedFile));
      targetEntry = (BibEntry) sourceEntry.clone();
      targetEntry.setFiles(List.of(linkedFile));

      sourceContext.getDatabase().insertEntry(sourceEntry);
      targetContext.getDatabase().insertEntry(targetEntry);
    }

    @Test
    void fileNotReachableAndPathsDiffer_ShouldCopyFileAndCreateDirectory() throws IOException {
      var returnedEntries = LinkedFileTransferHelper.adjustLinkedFilesForTarget(sourceContext, targetContext,
        filePreferences);

      assertEquals(1, returnedEntries.size());
      assertEquals("sourcefiles/test.pdf", sourceContext.getEntries().getFirst().getFiles().getFirst().getLink());
      assertEquals("sourcefiles/test.pdf",
        targetContext.getEntries().getFirst().getFiles().getFirst().getLink());
      Path expectedFile = targetDir.resolve("sourcefiles/test.pdf");
      assertTrue(Files.exists(expectedFile), "File should have been copied to target directory");
    }
  }
}
