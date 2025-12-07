package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
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
    private BibEntry badEntry;
    private BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final CliPreferences preferences = mock(CliPreferences.class);

    @BeforeEach
    void setUp(@TempDir Path tempFolder) {
        entry = new BibEntry().withCitationKey("asdf");
        badEntry = new BibEntry();
        databaseContext = new BibDatabaseContext();

        when(filePreferences.confirmDeleteLinkedFile()).thenReturn(true);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));

        this.tempFolder = tempFolder;
    }

    @ParameterizedTest(name = "{1} to {2} should be {0}")
    @CsvSource(textBlock = """
                newName.pdf, test.pdf, newName
                newName.txt, test.pdf, newName.txt
                newNameWithoutExtension, test, newNameWithoutExtension
                newName.pdf, testFile, newName.pdf
                newName..pdf, test.pdf, newName.
            """)
    void renameFile(String expectedFileName, String originalFileName, String newFileName) throws IOException {
        final Path tempFile = tempFolder.resolve(originalFileName);
        Files.createFile(tempFile);

        final LinkedFile linkedFile = new LinkedFile("", tempFile, "");
        LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        linkedFileHandler.renameToName(newFileName, false);
        final String result = Path.of(linkedFile.getLink()).getFileName().toString();
        assertEquals(expectedFileName, result);
    }

    @ParameterizedTest(name = "{1} with {2} should be {0} with citation key 'asdf'")
    @CsvSource(textBlock = """
                asdf.pdf, '', pdf
                asdf.pdf, https://example.com/file.pdf, pdf
                asdf.pdf, https://example.com/file.pdf?query=test, pdf
                asdf.pdf, https://example.com/file.doc, pdf
                asdf.pdf, https://example.com/file, pdf
                asdf.pdf, https://example.com/file.pdf, ''
                asdf.pdf, https://example.com/, pdf
                asdf.pdf, path/to/file.pdf, pdf
                asdf.pdf, https://www.cncf.io/wp-content/uploads/2020/08/OAM-Webinar-V2.pdf, pdf
            """)
    void getSuggestedFileName(String expectedFileName, String link, String extension) {
        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");

        final LinkedFile linkedFile = new LinkedFile("", link, "");
        final LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);

        if (extension.isEmpty()) {
            assertEquals(expectedFileName, linkedFileHandler.getSuggestedFileName(extension).orElse("file" + "." + extension));
        } else {
            assertEquals(expectedFileName, linkedFileHandler.getSuggestedFileName().orElse("file"));
        }
    }

    @ParameterizedTest(name = "{1} with {2} should be {0} with empty citation key")
    @CsvSource(textBlock = """
                file.pdf, '', pdf
                file.pdf, https://example.com/file.pdf, pdf
                file.pdf, https://example.com/file.pdf?query=test, pdf
                file.pdf, https://example.com/file.doc, pdf
                file.pdf, https://example.com/file, pdf
                file.pdf, https://example.com/file.pdf, ''
                file.pdf, https://example.com/, pdf
                file.pdf, path/to/file.pdf, pdf
                other.pdf, https://example.com/other.pdf, pdf
                other.pdf, https://example.com/other.pdf?query=test, pdf
                other.pdf, https://example.com/other.doc, pdf
                other.pdf, https://example.com/other, pdf
                other.pdf, https://example.com/other.pdf, ''
                other.pdf, path/to/other.pdf, pdf
                OAM-Webinar-V2.pdf, https://www.cncf.io/wp-content/uploads/2020/08/OAM-Webinar-V2.pdf, pdf
            """)
    void getSuggestedFileNameWithMissingKey(String expectedFileName, String link, String extension) {
        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");

        final LinkedFile linkedFile = new LinkedFile("", link, "");
        final LinkedFileHandler linkedFileHandler = new LinkedFileHandler(linkedFile, badEntry, databaseContext, filePreferences);

        if (extension.isEmpty()) {
            assertEquals(expectedFileName, linkedFileHandler.getSuggestedFileName(extension).orElse("file" + "." + extension));
        } else {
            assertEquals(expectedFileName, linkedFileHandler.getSuggestedFileName().orElse("file"));
        }
    }
}
