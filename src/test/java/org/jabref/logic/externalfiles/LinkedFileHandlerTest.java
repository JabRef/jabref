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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFileHandlerTest {
    private Path tempFolder;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        entry = new BibEntry().withCitationKey("asdf");
        databaseContext = new BibDatabaseContext();

        when(filePreferences.confirmDeleteLinkedFile()).thenReturn(true);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));

        this.tempFolder = tempFolder;
    }

    @ParameterizedTest(name = "{1} to {2} should be {0}")
    @CsvSource({
            "newName.pdf, test.pdf, newName",
            "newName.txt, test.pdf, newName.txt",
            "newNameWithoutExtension, test, newNameWithoutExtension",
            "newName.pdf, testFile, newName.pdf",
            "newName..pdf, test.pdf, newName."
    })
    void renameFile(String expectedFileName, String originalFileName, String newFileName) throws Exception {
        final Path tempFile = tempFolder.resolve(originalFileName);
        Files.createFile(tempFile);

        final LinkedFile linkedFile = new LinkedFile("", tempFile, "");
        LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        linkedFileHandler.renameToName(newFileName, false);
        final String result = Path.of(linkedFile.getLink()).getFileName().toString();
        assertEquals(expectedFileName, result);
    }

    @ParameterizedTest(name = "{1} with {2} should be {0}")
    @CsvSource({
            "asdf.pdf, '', pdf",
            "file.pdf, https://example.com/file.pdf, pdf",
            "file.pdf, https://example.com/file.pdf?query=test, pdf",
            "file.pdf, https://example.com/file.doc, pdf",
            "file.pdf, https://example.com/file, pdf",
            "file.pdf, https://example.com/file.pdf, ''",
            "-.pdf, https://example.com/, pdf",
            "-.pdf, path/to/file.pdf, pdf",
            "OAM-Webinar-V2.pdf, https://www.cncf.io/wp-content/uploads/2020/08/OAM-Webinar-V2.pdf, pdf"
    })
    void getSuggestedFileName(String expectedFileName, String link, String extension) {
        if (link.isEmpty()) {
            when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");
        } else {
            when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey] - [title]");
        }

        BibEntry testEntry = new BibEntry();
        if (link.isEmpty()) {
            testEntry = entry;
        }

        final LinkedFile linkedFile = mock(LinkedFile.class);
        when(linkedFile.isOnlineLink()).thenReturn(link.startsWith("http"));
        when(linkedFile.getLink()).thenReturn(link);

        final LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, testEntry, databaseContext, filePreferences);

        final String result = linkedFileHandler.getSuggestedFileName(extension);
        assertEquals(expectedFileName, result);
    }
}
