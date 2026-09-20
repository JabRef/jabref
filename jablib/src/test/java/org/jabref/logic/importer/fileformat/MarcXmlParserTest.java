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
import org.jabref.support.BibEntryAssert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    // [utest->req~import.dnb.marc-metadata~1]
    void importsDoiFromDnbMarcXml() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlDoiRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals("9783031996870", entries.getFirst().getField(StandardField.ISBN).orElseThrow());
            assertEquals("10.1007/978-3-031-99687-0", entries.getFirst().getField(StandardField.DOI).orElseThrow());
        }
    }

    @Test
    // [utest->req~import.dnb.marc-metadata~1]
    void importsParentJournalFromDnbMarcXml() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlParentJournalRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals("In: International Journal for Parasitology: Parasites and Wildlife (2025) 28:101137. https://doi.org/10.1016/j.ijppaw.2025.101137", entries.getFirst().getField(StandardField.JOURNAL).orElseThrow());
        }
    }

    @Test
    // [utest->req~import.dnb.marc-metadata~1]
    void ignoresDnbContentDescriptionUrl() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlContentDescriptionRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals(List.of(), entries.getFirst().getFiles());
            assertEquals(Optional.empty(), entries.getFirst().getField(StandardField.URL));
        }
    }

    @Test
    // [utest->req~import.dnb.marc-metadata~1]
    void importsDnbFulltextUrl() throws IOException, ParseException {
        try (InputStream inputStream = MarcXmlParserTest.class.getResourceAsStream("DnbMarcXmlFulltextRecord.xml")) {
            List<BibEntry> entries = new MarcXmlParser().parseEntries(inputStream);

            assertEquals(1, entries.size());
            assertEquals("https://d-nb.info/1415413312/34", entries.getFirst().getFiles().getFirst().getLink());
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
