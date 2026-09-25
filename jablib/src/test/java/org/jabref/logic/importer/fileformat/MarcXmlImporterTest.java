package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->req~import.marc21-xml~1]
// [utest->req~import.bibliographic.xml-formats~1]
@NullMarked
class MarcXmlImporterTest {
    private static final String RECORD = """
            <marc:record xmlns:marc="http://www.loc.gov/MARC21/slim">
              <marc:leader>00000nam a2200000 a 4500</marc:leader>
              <marc:datafield tag="245" ind1="0" ind2="0">
                <marc:subfield code="a">A standalone book</marc:subfield>
              </marc:datafield>
            </marc:record>
            """;

    private final MarcXmlImporter importer = new MarcXmlImporter();

    @Test
    void importsStandaloneRecordFromString() throws IOException {
        assertTrue(importer.isRecognizedFormat(RECORD));
        List<BibEntry> entries = importer.importDatabase(RECORD).getDatabase().getEntries();
        assertEquals(1, entries.size());
        assertEquals("A standalone book", entries.getFirst().getField(StandardField.TITLE).orElseThrow());
    }

    @Test
    void importsStandaloneRecordFromFile(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("book.xml");
        Files.writeString(file, RECORD);

        assertTrue(importer.isRecognizedFormat(file));
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(1, entries.size());
        assertEquals("A standalone book", entries.getFirst().getField(StandardField.TITLE).orElseThrow());
    }

    @Test
    void importsMultipleRecordsFromCollection() throws IOException {
        String collection = """
                <collection xmlns="http://www.loc.gov/MARC21/slim">
                  <record>
                    <leader>00000nam a2200000 a 4500</leader>
                    <datafield tag="245"><subfield code="a">First book</subfield></datafield>
                  </record>
                  <record>
                    <leader>00000nam a2200000 a 4500</leader>
                    <datafield tag="245"><subfield code="a">Second book</subfield></datafield>
                  </record>
                </collection>
                """;

        assertTrue(importer.isRecognizedFormat(collection));
        assertEquals(List.of("First book", "Second book"), importer.importDatabase(collection).getDatabase().getEntries().stream()
                                                               .map(entry -> entry.getField(StandardField.TITLE).orElseThrow())
                                                               .toList());
    }

    @Test
    void doesNotRecognizeOtherXml() throws IOException {
        String other = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>";
        assertFalse(importer.isRecognizedFormat(other));
        assertEquals(0, importer.importDatabase(other).getDatabase().getEntryCount());
    }

    @Test
    void malformedXmlReturnsInvalidResult() throws IOException {
        ParserResult result = importer.importDatabase("<record><leader>");

        assertTrue(result.isInvalid());
        assertEquals(0, result.getDatabase().getEntryCount());
    }

    @Test
    void rejectsExternalEntity() throws IOException {
        String xml = """
                <!DOCTYPE record [<!ENTITY external SYSTEM "file:///not-accessed">]>
                <record><leader>&external;</leader></record>
                """;

        assertFalse(importer.isRecognizedFormat(xml));
        ParserResult result = importer.importDatabase(xml);
        assertTrue(result.isInvalid());
        assertEquals(0, result.getDatabase().getEntryCount());
    }
}
