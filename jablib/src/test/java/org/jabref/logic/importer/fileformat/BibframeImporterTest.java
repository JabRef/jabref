package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
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
        BibEntry expectedBook = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Marketing Automation with Mailchimp")
                .withField(StandardField.SUBTITLE, "Expert Tips")
                .withField(StandardField.AUTHOR, "Caraballo, Margarita J.")
                .withField(StandardField.ADDRESS, "Birmingham")
                .withField(StandardField.PUBLISHER, "Packt Publishing")
                .withField(StandardField.EDITION, "2nd ed.")
                .withField(StandardField.PAGETOTAL, "240")
                .withField(StandardField.SERIES, "Practical computing")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.ISBN, "9781800567566")
                .withField(StandardField.DOI, "10.1234/book.001")
                .withField(StandardField.LANGUAGE, "eng")
                .withField(StandardField.ABSTRACT, "A guide to marketing automation.")
                .withField(StandardField.URL, "https://example.org/book")
                .withFiles(List.of(new LinkedFile("", "https://example.org/book", StandardFileType.PDF)));

        assertEquals(List.of(expectedBook), entries);
    }

    @Test
    void importsOfficialArticleFixture() throws Exception {
        Path file = Path.of(BibframeImporterTest.class.getResource("/org/jabref/logic/importer/bibframe/article.rdf").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry expectedArticle = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Article 22 of the African Charter")
                .withField(StandardField.AUTHOR, "Bello, Emmanuel G.")
                .withField(StandardField.JOURNAL, "Journal of African Law")
                .withField(StandardField.ISSN, "0021-8553")
                .withField(StandardField.PAGES, "447-473")
                .withField(StandardField.YEAR, "1992")
                .withField(StandardField.DOI, "10.1234/article.001")
                .withField(StandardField.LANGUAGE, "eng");

        assertEquals(List.of(expectedArticle), entries);
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
        assertEquals(List.of(
                        new BibEntry().withField(StandardField.TITLE, "First work"),
                        new BibEntry().withField(StandardField.TITLE, "Second work")),
                importer.importDatabase(xml).getDatabase().getEntries());
    }

    @Test
    void importsLocStyleExtentSupplementaryUrlAndSecondaryFile() throws IOException {
        String xml = """
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                         xmlns:bf="http://id.loc.gov/ontologies/bibframe/">
                  <bf:Work rdf:about="https://example.org/work">
                    <rdf:type rdf:resource="http://id.loc.gov/ontologies/bibframe/Monograph"/>
                    <bf:hasInstance rdf:resource="https://example.org/instance"/>
                    <bf:hasInstance rdf:resource="https://example.org/file"/>
                  </bf:Work>
                  <bf:Instance rdf:about="https://example.org/instance">
                    <bf:title><bf:Title><bf:mainTitle>Library book</bf:mainTitle></bf:Title></bf:title>
                    <bf:extent><bf:Extent><rdfs:label>xii, 267 p.</rdfs:label></bf:Extent></bf:extent>
                    <bf:supplementaryContent><bf:SupplementaryContent>
                      <bf:electronicLocator rdf:resource="https://example.org/description"/>
                    </bf:SupplementaryContent></bf:supplementaryContent>
                    <bf:instanceOf rdf:resource="https://example.org/work"/>
                  </bf:Instance>
                  <bf:Instance rdf:about="https://example.org/file">
                    <rdf:type rdf:resource="http://id.loc.gov/ontologies/bflc/SecondaryInstance"/>
                    <bf:title><bf:Title><bf:mainTitle>Full text</bf:mainTitle></bf:Title></bf:title>
                    <bf:electronicLocator rdf:resource="https://example.org/book.pdf"/>
                    <bf:instanceOf rdf:resource="https://example.org/work"/>
                  </bf:Instance>
                </rdf:RDF>
                """;

        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Library book")
                .withField(StandardField.PAGETOTAL, "267")
                .withField(StandardField.URL, "https://example.org/description")
                .withFiles(List.of(new LinkedFile("Full text", "https://example.org/book.pdf", StandardFileType.PDF)));

        assertEquals(List.of(expected), importer.importDatabase(xml).getDatabase().getEntries());
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
