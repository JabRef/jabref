package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->req~import.bibliographic.xml-formats~1]
@NullMarked
class BibframeImporterTest {
    private final BibframeImporter importer = new BibframeImporter();

    @Test
    void importsOfficialBookFixture() throws Exception {
        Path file = Path.of(BibframeImporterTest.class.getResource("/org/jabref/logic/importer/bibframe/book.rdf").toURI());
        assertTrue(importer.isRecognizedFormat(file));

        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(1, entries.size());
        BibEntry book = entries.getFirst();
        assertEquals(StandardEntryType.Book, book.getType());
        assertEquals("Marketing Automation with Mailchimp", book.getField(StandardField.TITLE).orElseThrow());
        assertEquals("Expert Tips", book.getField(StandardField.SUBTITLE).orElseThrow());
        assertEquals("Caraballo, Margarita J.", book.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals("Birmingham", book.getField(StandardField.ADDRESS).orElseThrow());
        assertEquals("Packt Publishing", book.getField(StandardField.PUBLISHER).orElseThrow());
        assertEquals("2023", book.getField(StandardField.YEAR).orElseThrow());
        assertEquals("9781800567566", book.getField(StandardField.ISBN).orElseThrow());
        assertEquals("10.1234/book.001", book.getField(StandardField.DOI).orElseThrow());
        assertEquals("eng", book.getField(StandardField.LANGUAGE).orElseThrow());
        assertEquals("A guide to marketing automation.", book.getField(StandardField.ABSTRACT).orElseThrow());
        assertEquals("https://example.org/book", book.getField(StandardField.URL).orElseThrow());
    }

    @Test
    void importsOfficialArticleFixture() throws Exception {
        Path file = Path.of(BibframeImporterTest.class.getResource("/org/jabref/logic/importer/bibframe/article.rdf").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(1, entries.size());
        BibEntry article = entries.getFirst();
        assertEquals(StandardEntryType.Article, article.getType());
        assertEquals("Article 22 of the African Charter", article.getField(StandardField.TITLE).orElseThrow());
        assertEquals("Bello, Emmanuel G.", article.getField(StandardField.AUTHOR).orElseThrow());
        assertEquals("Journal of African Law", article.getField(StandardField.JOURNAL).orElseThrow());
        assertEquals("0021-8553", article.getField(StandardField.ISSN).orElseThrow());
        assertEquals("447-473", article.getField(StandardField.PAGES).orElseThrow());
        assertEquals("1992", article.getField(StandardField.YEAR).orElseThrow());
        assertEquals("10.1234/article.001", article.getField(StandardField.DOI).orElseThrow());
    }

    @Test
    void importsTwoLinkedDescriptionsWithAlternatePrefixes() throws IOException {
        String xml = """
                <r:RDF xmlns:r="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                       xmlns:b="http://id.loc.gov/ontologies/bibframe/">
                  <r:Description r:about="https://example.org/one#Work">
                    <r:type r:resource="http://id.loc.gov/ontologies/bibframe/Work"/>
                    <b:title><b:Title><b:mainTitle>First work</b:mainTitle></b:Title></b:title>
                    <b:hasInstance r:resource="https://example.org/one#Instance"/>
                  </r:Description>
                  <r:Description r:about="https://example.org/two#Work">
                    <r:type r:resource="http://id.loc.gov/ontologies/bibframe/Work"/>
                    <b:title><b:Title><b:mainTitle>Second work</b:mainTitle></b:Title></b:title>
                    <b:hasInstance r:resource="https://example.org/two#Instance"/>
                  </r:Description>
                  <r:Description r:about="https://example.org/one#Instance">
                    <r:type r:resource="http://id.loc.gov/ontologies/bibframe/Instance"/>
                    <b:instanceOf r:resource="https://example.org/one#Work"/>
                  </r:Description>
                  <r:Description r:about="https://example.org/two#Instance">
                    <r:type r:resource="http://id.loc.gov/ontologies/bibframe/Instance"/>
                    <b:instanceOf r:resource="https://example.org/two#Work"/>
                  </r:Description>
                </r:RDF>
                """;

        assertTrue(importer.isRecognizedFormat(xml));
        assertEquals(List.of("First work", "Second work"), importer.importDatabase(xml).getDatabase().getEntries().stream()
                                                                .map(entry -> entry.getField(StandardField.TITLE).orElseThrow())
                                                                .toList());
    }

    @Test
    void rejectsMalformedXmlAndExternalEntities() throws IOException {
        assertTrue(importer.importDatabase("<rdf:RDF>").isInvalid());
        String xml = """
                <!DOCTYPE rdf:RDF [<!ENTITY external SYSTEM "file:///not-accessed">]>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <bf:Instance xmlns:bf="http://id.loc.gov/ontologies/bibframe/">
                    <bf:title>&external;</bf:title>
                  </bf:Instance>
                </rdf:RDF>
                """;

        assertFalse(importer.isRecognizedFormat(xml));
        ParserResult result = importer.importDatabase(xml);
        assertTrue(result.isInvalid());
        assertEquals(0, result.getDatabase().getEntryCount());
    }

    @Test
    void doesNotRecognizeOtherRdf() throws IOException {
        String bibo = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:bibo="http://purl.org/ontology/bibo/">
                  <bibo:Book rdf:about="https://example.org/book"/>
                </rdf:RDF>
                """;
        assertFalse(importer.isRecognizedFormat(bibo));
    }
}
