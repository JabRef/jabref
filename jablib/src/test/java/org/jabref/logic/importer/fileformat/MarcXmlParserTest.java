package org.jabref.logic.importer.fileformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.BibEntryAssert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [utest->req~import.marc21-xml~1]
class MarcXmlParserTest {

    private static final String FILE_ENDING = ".xml";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MarcXMLParserTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private void doTest(String xmlName, String bibName) throws IOException, ParseException {
        try (InputStream is = MarcXmlParserTest.class.getResourceAsStream(xmlName)) {
            MarcXmlParser parser = new MarcXmlParser();
            List<BibEntry> entries = parser.parseEntries(is);
            assertNotNull(entries);
            BibEntryAssert.assertEquals(MarcXmlParserTest.class, bibName, entries.getFirst());
        }
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void importEntries(String fileName) throws IOException, ParseException {
        String bibName = FileUtil.getBaseName(fileName) + ".bib";
        doTest(fileName, bibName);
    }

    @Test
    void importsDoiFromDnbMarcXml() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlDoiRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals("9783031996870", entries.getFirst().getField(StandardField.ISBN).orElseThrow());
            assertEquals("10.1007/978-3-031-99687-0", entries.getFirst().getField(StandardField.DOI).orElseThrow());
        }
    }

    @Test
    void prefersIsbn13RegardlessOfHyphenatedIsbnOrder() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlIsbnOrderingRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(2, entries.size());
            assertEquals("9783570184783", entries.getFirst().getField(StandardField.ISBN).orElseThrow());
            assertEquals("9783570184783", entries.get(1).getField(StandardField.ISBN).orElseThrow());
        }
    }

    @Test
    void importsParentJournalFromDnbMarcXml() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlParentJournalRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals(StandardEntryType.Article, entries.getFirst().getType());
            assertEquals("Seminars in speech and language", entries.getFirst().getField(StandardField.JOURNAL).orElseThrow());
        }
    }

    @Test
    void doesNotChangeMonographicPartToArticle() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlMonographicPartRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals(StandardEntryType.Misc, entries.getFirst().getType());
            assertEquals(Optional.empty(), entries.getFirst().getField(StandardField.JOURNAL));
        }
    }

    @Test
    void ignoresDnbContentDescriptionUrl() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlContentDescriptionRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals(List.of(), entries.getFirst().getFiles());
            assertEquals(Optional.empty(), entries.getFirst().getField(StandardField.URL));
        }
    }

    @Test
    void importsDnbFulltextUrl() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlFulltextRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals("https://www.swp-berlin.org/publications/products/aktuell/2026A25_Europaeische_Staatsanwaltschaft.pdf", entries.getFirst().getFiles().getFirst().getLink());
            assertEquals(Optional.empty(), entries.getFirst().getField(StandardField.URL));
        }
    }

    @Test
    void rejectsDocumentTypeDeclarations() {
        String xmlWithDoctype = """
                <!DOCTYPE response [<!ENTITY entity SYSTEM "file:///not-accessed">]>
                <zs:searchRetrieveResponse xmlns:zs="http://www.loc.gov/zing/srw/"/>
                """;

        MarcXmlParser parser = new MarcXmlParser();

        assertThrows(ParseException.class, () -> parser.parseEntries(new ByteArrayInputStream(xmlWithDoctype.getBytes(StandardCharsets.UTF_8))));
    }
}
