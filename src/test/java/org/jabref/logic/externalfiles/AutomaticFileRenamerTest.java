package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for automatic file renaming via event listening */
class AutomaticFileRenamerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticFileRenamerTest.class);

    private BibDatabaseContext databaseContext;
    private FilePreferences filePreferences;
    private AutomaticFileRenamer sut;
    private BibEntry entry;
    private Path oldFile;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        this.tempDir = tempDir;

        MetaData metaData = new MetaData();
        metaData.setLibrarySpecificFileDirectory(tempDir.toString());
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData);

        filePreferences = new FilePreferences(
                "", 
                tempDir.toString(), 
                true, 
                "[citationkey]", 
                "", 
                false, 
                true, 
                Path.of(""), 
                false, 
                Path.of(""), 
                true, 
                false, 
                false, 
                true // auto rename enabled for tests
        );

        entry = createTestEntryWithFile(tempDir);
        databaseContext.getDatabase().insertEntry(entry);

        sut = new AutomaticFileRenamer(databaseContext, filePreferences);
    }

    private BibEntry createTestEntryWithFile(Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("oldKey")
                .withField(StandardField.AUTHOR, "Author")
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.YEAR, "2020");

        oldFile = tempDir.resolve("oldKey.pdf");
        Files.deleteIfExists(oldFile);
        Files.createFile(oldFile);

        LinkedFile linkedFile = new LinkedFile("", "oldKey.pdf", "PDF");
        entry.setFiles(List.of(linkedFile));

        return entry;
    }

    @Test
    void citationKeyChangeShouldRenameFileViaListener() throws Exception {
        databaseContext.getDatabase().registerListener(sut);

        entry.setCitationKey("newKey");

        Thread.sleep(1000);

        Path newFile = tempDir.resolve("newKey.pdf");
        assertFileRenamed(oldFile, newFile);
    }

    @Test
    void autoRenameDisabledShouldNotRenameFile() throws Exception {
        FilePreferences disabledAutoRenamePrefs = new FilePreferences(
                "", 
                tempDir.toString(), 
                true, 
                "[citationkey]", 
                "", 
                false, 
                true, 
                Path.of(""), 
                false, 
                Path.of(""), 
                true, 
                false, 
                false, 
                false // auto rename DISABLED
        );

        AutomaticFileRenamer disabledRenamer = new AutomaticFileRenamer(databaseContext, disabledAutoRenamePrefs);
        
        databaseContext.getDatabase().registerListener(disabledRenamer);
        entry.setCitationKey("newKey");
        Thread.sleep(1000);

        assertTrue(Files.exists(oldFile), "Old file should still exist when auto-rename is disabled");
        assertEquals("oldKey.pdf", entry.getFiles().getFirst().getLink(), 
                "File link should remain unchanged when auto-rename is disabled");
    }

    @Test
    void nonKeyFieldChangeShouldNotRenameFile() throws Exception {
        databaseContext.getDatabase().registerListener(sut);
        entry.setField(StandardField.TITLE, "New Title");
        Thread.sleep(1000);

        assertTrue(Files.exists(oldFile), "Old file should still exist when changing non-key fields");
        assertEquals("oldKey.pdf", Path.of(entry.getFiles().getFirst().getLink()).getFileName().toString(),
                "File link should not change when modifying non-key fields");
    }

    private void assertFileRenamed(Path oldFile, Path newFile) throws IOException {
        assertFalse(Files.exists(oldFile), "Old file should not exist: " + oldFile);
        assertTrue(Files.exists(newFile), "New file should exist: " + newFile);
        assertTrue(
                entry.getFiles().stream()
                     .anyMatch(file -> file.getLink().equals(newFile.getFileName().toString()) ||
                             file.getLink().endsWith("/" + newFile.getFileName().toString())),
                "Entry's linked file should be updated to: " + newFile.getFileName()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.cleanDirectory(tempDir.toFile());
    }
}
