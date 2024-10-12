package org.jabref.logic.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFileHandlerTest {
    private Path tempFolder;
    private Path tempFile;
    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        entry = new BibEntry().withCitationKey("asdf");
        databaseContext = new BibDatabaseContext();

        // Mock preferences
        when(filePreferences.confirmDeleteLinkedFile()).thenReturn(true);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));

        this.tempFolder = tempFolder;
    }

    void createFile(String fileName) throws Exception {
        tempFile = tempFolder.resolve(fileName);
        Files.createFile(tempFile);
    }

    @Test
    void keepOldExtensionIfNewIsNotProvided() throws Exception {
        createFile("test.pdf");
        linkedFile = new LinkedFile("", tempFile, "");
        LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        linkedFileHandler.renameToName("newName", true);
        final String result = Path.of(linkedFile.getLink()).getFileName().toString();
        assertEquals("newName.pdf", result, "File should retain its original name with extension");
   }

    @Test
    void renameFileWithNewNameAndExtension() throws Exception {
        createFile("test.pdf");
        linkedFile = new LinkedFile("", tempFile, "");
        LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        linkedFileHandler.renameToName("newName.txt", false);

        final String result = Path.of(linkedFile.getLink()).getFileName().toString();
        assertEquals("newName.txt", result, "File should be renamed to newName.txt");
    }

    @Test
    void doNotRenameFileWithoutAnyExtension() throws Exception {
        createFile("test");
        linkedFile = new LinkedFile("", tempFile, "");
        LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        linkedFileHandler.renameToName("newNameWithoutExtension", false);
        final String result = Path.of(linkedFile.getLink()).getFileName().toString();
        assertEquals("test", result, "File should not be renamed without extension");
    }

    @Test
    void renameAddsMissingExtension() throws Exception {
        createFile("testFile");
        linkedFile = new LinkedFile("", tempFile, "");
        LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        linkedFileHandler.renameToName("newName.pdf", false);
        final String result = Path.of(linkedFile.getLink()).getFileName().toString();
        assertEquals("newName.pdf", result, "File without extension should be renamed correctly");
    }

    @Test
    void renameWithDotAndWithoutNewExtension() throws Exception {
        createFile("test.pdf");
        linkedFile = new LinkedFile("", tempFile, "");
        LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        linkedFileHandler.renameToName("newName.", false);
        final String result = Path.of(linkedFile.getLink()).getFileName().toString();

        assertEquals("newName..pdf", result, "File without extension should be renamed correctly");
    }
}
