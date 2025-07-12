package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoRenameFileOnEntryChangeTest {
    private FilePreferences filePreferences;
    private BibEntry entry;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        MetaData metaData = new MetaData();
        metaData.setLibrarySpecificFileDirectory(tempDir.toString());
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), metaData);
        GlobalCitationKeyPatterns keyPattern = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
        GuiPreferences guiPreferences = mock(GuiPreferences.class);
        filePreferences = mock(FilePreferences.class);
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(
                false,
                true,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                keyPattern,
                "",
                ',');

        when(guiPreferences.getCitationKeyPatternPreferences()).thenReturn(patternPreferences);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");

        entry = new BibEntry(StandardEntryType.Article).withCitationKey("oldKey2081")
                .withField(StandardField.AUTHOR, "oldKey")
                .withField(StandardField.YEAR, "2081");

        bibDatabaseContext.getDatabase().insertEntry(entry);
        new AutoRenameFileOnEntryChange(bibDatabaseContext, guiPreferences);
    }

    @Test
    void noFileRenameByDefault() throws IOException {
        Files.createFile(tempDir.resolve("oldKey2081.pdf"));
        entry.setFiles(List.of(new LinkedFile("", "oldKey2081.pdf", "PDF")));
        entry.setField(StandardField.AUTHOR, "newKey");

        assertEquals("oldKey2081.pdf", entry.getFiles().getFirst().getLink());
        assertTrue(Files.exists(tempDir.resolve("oldKey2081.pdf")));
    }

    @Test
    void noFileRenameOnEmptyFilePattern() throws IOException {
        Files.createFile(tempDir.resolve("oldKey2081.pdf"));
        entry.setFiles(List.of(new LinkedFile("", "oldKey2081.pdf", "PDF")));
        when(filePreferences.getFileNamePattern()).thenReturn("");
        when(filePreferences.shouldAutoRenameFilesOnChange()).thenReturn(true);
        entry.setField(StandardField.AUTHOR, "newKey");

        assertEquals("oldKey2081.pdf", entry.getFiles().getFirst().getLink());
        assertTrue(Files.exists(tempDir.resolve("oldKey2081.pdf")));
    }

    @Test
    void singleFileRenameOnEntryChange() throws IOException {
        Files.createFile(tempDir.resolve("oldKey2081.pdf"));
        entry.setFiles(List.of(new LinkedFile("", "oldKey2081.pdf", "PDF")));
        when(filePreferences.shouldAutoRenameFilesOnChange()).thenReturn(true);

        // change author only
        entry.setField(StandardField.AUTHOR, "newKey");
        assertEquals("newKey2081.pdf", entry.getFiles().getFirst().getLink());
        assertTrue(Files.exists(tempDir.resolve("newKey2081.pdf")));

        // change year only
        entry.setField(StandardField.YEAR, "2082");
        assertEquals("newKey2082.pdf", entry.getFiles().getFirst().getLink());
        assertTrue(Files.exists(tempDir.resolve("newKey2082.pdf")));
    }

    @Test
    void multipleFilesRenameOnEntryChange() throws IOException {
        // create multiple entries
        List<String> fileNames = Arrays.asList(
                "oldKey2081.pdf",
                "oldKey2081.jpg",
                "oldKey2081.csv",
                "oldKey2081.doc",
                "oldKey2081.docx"
        );

        for (String fileName : fileNames) {
            Path filePath = tempDir.resolve(fileName);
            Files.createFile(filePath);
        }

        LinkedFile pdfLinkedFile = new LinkedFile("", "oldKey2081.pdf", "PDF");
        LinkedFile jpgLinkedFile = new LinkedFile("", "oldKey2081.jpg", "JPG");
        LinkedFile csvLinkedFile = new LinkedFile("", "oldKey2081.csv", "CSV");
        LinkedFile docLinkedFile = new LinkedFile("", "oldKey2081.doc", "DOC");
        LinkedFile docxLinkedFile = new LinkedFile("", "oldKey2081.docx", "DOCX");

        entry.setFiles(List.of(pdfLinkedFile, jpgLinkedFile, csvLinkedFile, docLinkedFile, docxLinkedFile));
        when(filePreferences.shouldAutoRenameFilesOnChange()).thenReturn(true);

        // Change author only
        entry.setField(StandardField.AUTHOR, "newKey");
        assertTrue(Files.exists(tempDir.resolve("newKey2081.pdf")));
        assertTrue(Files.exists(tempDir.resolve("newKey2081.jpg")));
        assertTrue(Files.exists(tempDir.resolve("newKey2081.csv")));
        assertTrue(Files.exists(tempDir.resolve("newKey2081.doc")));
        assertTrue(Files.exists(tempDir.resolve("newKey2081.docx")));

        // change year only
        entry.setField(StandardField.YEAR, "2082");
        assertTrue(Files.exists(tempDir.resolve("newKey2082.pdf")));
        assertTrue(Files.exists(tempDir.resolve("newKey2082.jpg")));
        assertTrue(Files.exists(tempDir.resolve("newKey2082.csv")));
        assertTrue(Files.exists(tempDir.resolve("newKey2082.doc")));
        assertTrue(Files.exists(tempDir.resolve("newKey2082.docx")));
    }
}
