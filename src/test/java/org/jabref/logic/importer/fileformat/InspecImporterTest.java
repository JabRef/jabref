package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InspecImporterTest {

    private static final String FILE_ENDING = ".txt";
    private InspecImporter importer;

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("InspecImportTest")
                && !name.contains("False")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> nonInspecfileNames() throws IOException {
        Predicate<String> fileName = name -> !name.startsWith("InspecImportTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @BeforeEach
    void setUp() throws Exception {
        this.importer = new InspecImporter();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormatAccept(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(importer, fileName);
    }

    @ParameterizedTest
    @MethodSource("nonInspecfileNames")
    void isRecognizedFormatReject(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(importer, fileName);
    }

    @Test
    void completeBibtexEntryOnJournalPaperImport() throws IOException, URISyntaxException {
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article);
        expectedEntry.setField(StandardField.TITLE, "The SIS project : software reuse with a natural language approach");
        expectedEntry.setField(StandardField.AUTHOR, "Prechelt, Lutz");
        expectedEntry.setField(StandardField.YEAR, "1992");
        expectedEntry.setField(StandardField.ABSTRACT, "Abstrakt");
        expectedEntry.setField(StandardField.KEYWORDS, "key");
        expectedEntry.setField(StandardField.JOURNAL, "10000");
        expectedEntry.setField(StandardField.PAGES, "20");
        expectedEntry.setField(StandardField.VOLUME, "19");

        BibEntryAssert.assertEquals(Collections.singletonList(expectedEntry),
                InspecImporterTest.class.getResource("InspecImportTest2.txt"), importer);
    }

    @Test
    void importConferencePaperGivesInproceedings() throws IOException {
        String testInput = "Record.*INSPEC.*\n" +
                "\n" +
                "RT ~ Conference-Paper\n" +
                "AU ~ Prechelt, Lutz";
        BibEntry expectedEntry = new BibEntry(StandardEntryType.InProceedings);
        expectedEntry.setField(StandardField.AUTHOR, "Prechelt, Lutz");

        try (BufferedReader reader = new BufferedReader(new StringReader(testInput))) {
            List<BibEntry> entries = importer.importDatabase(reader).getDatabase().getEntries();
            assertEquals(Collections.singletonList(expectedEntry), entries);
        }
    }

    @Test
    void importMiscGivesMisc() throws IOException {
        String testInput = "Record.*INSPEC.*\n" +
                "\n" +
                "AU ~ Prechelt, Lutz \n" +
                "RT ~ Misc";
        BibEntry expectedEntry = new BibEntry(StandardEntryType.Misc);
        expectedEntry.setField(StandardField.AUTHOR, "Prechelt, Lutz");

        try (BufferedReader reader = new BufferedReader(new StringReader(testInput))) {
            List<BibEntry> entries = importer.importDatabase(reader).getDatabase().getEntries();
            assertEquals(1, entries.size());
            BibEntry entry = entries.getFirst();
            assertEquals(expectedEntry, entry);
        }
    }

    @Test
    void getFormatName() {
        assertEquals("INSPEC", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("inspec", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.TXT, importer.getFileType());
    }
}
