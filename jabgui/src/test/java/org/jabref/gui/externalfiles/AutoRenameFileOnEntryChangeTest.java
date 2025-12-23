package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;

import com.google.common.eventbus.Subscribe;
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
        AutoRenameFileOnEntryChange autoRenameFileOnEntryChange = new AutoRenameFileOnEntryChange(bibDatabaseContext, guiPreferences.getFilePreferences());
        bibDatabaseContext.getDatabase().registerListener(autoRenameFileOnEntryChange);

        // Update citation-key when author/year changes
        bibDatabaseContext.getDatabase().registerListener(new Object() {
            @Subscribe
            public void listen(FieldChangedEvent event) {
                if (event.getField().equals(StandardField.AUTHOR)) {
                    String author = event.getNewValue();
                    String year = entry.getField(StandardField.YEAR).orElse("");
                    entry.setCitationKey(author + year);
                } else if (event.getField().equals(StandardField.YEAR)) {
                    String author = entry.getField(StandardField.AUTHOR).orElse("");
                    String year = event.getNewValue();
                    entry.setCitationKey(author + year);
                }
            }
        });
    }

    @Test
    void noFileRenameByDefault() throws IOException {
        Files.createFile(tempDir.resolve("oldKey2081.pdf"));
        entry.setFiles(List.of(new LinkedFile("", "oldKey2081.pdf", "PDF")));
        entry.setField(StandardField.AUTHOR, "newKey");

        assertEquals("oldKey2081.pdf", entry.getFiles().getFirst().getLink());
        assertFileExists(tempDir.resolve("oldKey2081.pdf"));
    }

    @Test
    void noFileRenameOnEmptyFilePattern() throws IOException {
        Files.createFile(tempDir.resolve("oldKey2081.pdf"));
        entry.setFiles(List.of(new LinkedFile("", "oldKey2081.pdf", "PDF")));
        when(filePreferences.getFileNamePattern()).thenReturn("");
        when(filePreferences.shouldAutoRenameFilesOnChange()).thenReturn(true);
        entry.setField(StandardField.AUTHOR, "newKey");

        assertEquals("oldKey2081.pdf", entry.getFiles().getFirst().getLink());
        assertFileExists(tempDir.resolve("oldKey2081.pdf"));
    }

    @Test
    void singleFileRenameOnEntryChange() throws IOException {
        Files.createFile(tempDir.resolve("oldKey2081.pdf"));
        entry.setFiles(List.of(new LinkedFile("", "oldKey2081.pdf", "PDF")));
        when(filePreferences.shouldAutoRenameFilesOnChange()).thenReturn(true);

        // change author only
        entry.setField(StandardField.AUTHOR, "newKey");
        assertEquals("newKey2081.pdf", entry.getFiles().getFirst().getLink());
        assertFileExists(tempDir.resolve("newKey2081.pdf"));

        // change year only
        entry.setField(StandardField.YEAR, "2082");
        assertEquals("newKey2082.pdf", entry.getFiles().getFirst().getLink());
        assertFileExists(tempDir.resolve("newKey2082.pdf"));
    }

    @Test
    void multipleFilesRenameOnEntryChange() throws IOException {
        // create multiple entries
        List<String> fileNames = List.of(
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
        assertFileExists(tempDir.resolve("newKey2081.pdf"));
        assertFileExists(tempDir.resolve("newKey2081.jpg"));
        assertFileExists(tempDir.resolve("newKey2081.csv"));
        assertFileExists(tempDir.resolve("newKey2081.doc"));
        assertFileExists(tempDir.resolve("newKey2081.docx"));

        // change year only
        entry.setField(StandardField.YEAR, "2082");
        assertFileExists(tempDir.resolve("newKey2082.pdf"));
        assertFileExists(tempDir.resolve("newKey2082.jpg"));
        assertFileExists(tempDir.resolve("newKey2082.csv"));
        assertFileExists(tempDir.resolve("newKey2082.doc"));
        assertFileExists(tempDir.resolve("newKey2082.docx"));
    }

    @Test
    void shouldHandleFileNameConflicts() throws IOException {
        // files that may or may not be linked to another entry
        Files.createFile(tempDir.resolve("newKey2081.pdf"));
        Files.createFile(tempDir.resolve("newKey2081 (1).pdf"));

        Files.createFile(tempDir.resolve("oldKey2081.pdf"));
        entry.setFiles(List.of(new LinkedFile("", "oldKey2081.pdf", "PDF")));
        when(filePreferences.shouldAutoRenameFilesOnChange()).thenReturn(true);

        entry.setField(StandardField.AUTHOR, "newKey");
        assertFileExists(tempDir.resolve("newKey2081.pdf"));
        assertFileExists(tempDir.resolve("newKey2081 (1).pdf"));
        assertFileExists(tempDir.resolve("newKey2081 (2).pdf"));
        assertEquals("newKey2081 (2).pdf", entry.getFiles().getFirst().getLink());
    }

    static void assertFileExists(Path path) throws IOException {
        assertTrue(Files.exists(path), "searched for " + path.getFileName().toString() + " but found " + Files.list(path.getParent()).map(f -> "'" + f.getFileName().toString() + "'").collect(Collectors.joining(", ")));
    }
}
