package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class EpubImporterFilesTest {
    private static final String FILE_ENDING = ".epub";

    private EpubImporter importer;

    @BeforeEach
    void setUp() throws XPathExpressionException, ParserConfigurationException {
        BibEntryPreferences bibEntryPreferences = new BibEntryPreferences(',');

        ImportFormatPreferences importFormatPreferences = new ImportFormatPreferences(
                bibEntryPreferences,
                mock(CitationKeyPatternPreferences.class),
                mock(FieldPreferences.class),
                mock(XmpPreferences.class),
                mock(DOIPreferences.class),
                mock(GrobidPreferences.class)
        );

        this.importer = new EpubImporter(importFormatPreferences);
    }

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("EpubImporterTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        // Clashes with ZIP-based formats are inevitable.
        Predicate<String> fileName = name -> !name.startsWith("EpubImporterTest") && !name.startsWith("CitaviXmlImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormat(String fileName) throws IOException, XPathExpressionException, ParserConfigurationException {
        ImporterTestEngine.testIsRecognizedFormat(importer, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void isNotRecognizedFormat(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(importer, fileName);
    }

    // Grimms were taken from Project Gutenberg. They were chosen as the book has 2 authors.

    @Test
    void grimmEpubOld() throws URISyntaxException, IOException {
        Path file = Path.of(EpubImporterFilesTest.class.getResource("EpubImporterTest1Old.epub").toURI());

        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(Collections.singletonList(grimms(file)), result);
    }

    @Test
    void grimmEpub3() throws URISyntaxException, IOException {
        Path file = Path.of(EpubImporterFilesTest.class.getResource("EpubImporterTest2Epub3.epub").toURI());

        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(Collections.singletonList(grimms(file)), result);
    }

    // Both ePUB3 and older ePUB version should have the same {@link BibEntry} (expect `file` field).
    BibEntry grimms(Path file) {
        BibEntry expected = new BibEntry(StandardEntryType.Book);
        expected.setField(StandardField.AUTHOR, "Jacob Grimm and Wilhelm Grimm");
        expected.setField(StandardField.TITLE, "Grimms' Fairy Tales");
        expected.setField(StandardField.KEYWORDS, "Fairy tales -- Germany");
        expected.setField(StandardField.LANGUAGE, "en");
        expected.setField(StandardField.URL, "https://www.gutenberg.org/files/2591/2591-h/2591-h.htm");

        expected.setFiles(List.of(new LinkedFile("", file.toAbsolutePath(), StandardFileType.EPUB.getName())));

        return expected;
    }
}
