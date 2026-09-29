package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.fileformat.BibframeImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->req~export.bibframe.rdfxml~1]
@NullMarked
class BibframeExporterTest {
    private final BibframeExporter exporter = new BibframeExporter();
    private final BibframeImporter importer = new BibframeImporter();

    @Test
    void officialFixturesRetainMappedFieldsOnExportAndImport(@TempDir Path directory) throws Exception {
        for (String name : List.of("book", "article")) {
            Path fixture = Path.of(BibframeExporterTest.class.getResource(
                    "/org/jabref/logic/importer/bibframe/" + name + ".rdf").toURI());
            BibEntry original = importer.importDatabase(fixture).getDatabase().getEntries().getFirst();
            Path output = directory.resolve(name + ".rdf");

            exporter.export(new BibDatabaseContext(), output, List.of(original));
            BibEntry restored = importer.importDatabase(output).getDatabase().getEntries().getFirst();

            assertEquals(original, restored);

            Path reexported = directory.resolve(name + "-reexported.rdf");
            exporter.export(new BibDatabaseContext(), reexported, List.of(restored));
            assertFalse(DiffBuilder.compare(Files.readString(output))
                                   .withTest(Files.readString(reexported))
                                   .ignoreWhitespace()
                                   .checkForSimilar()
                                   .build().hasDifferences());
        }
    }

    @Test
    void identifiersAreStableAndIndependentOfCitationKeys(@TempDir Path directory) throws SaveException, IOException {
        BibEntry first = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "A book").withCitationKey("first-key");
        BibEntry second = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "A book").withCitationKey("other-key");
        Path firstFile = directory.resolve("first.rdf");
        Path secondFile = directory.resolve("second.rdf");

        exporter.export(new BibDatabaseContext(), firstFile, List.of(first));
        exporter.export(new BibDatabaseContext(), secondFile, List.of(second));

        assertEquals(Files.readString(firstFile), Files.readString(secondFile));
    }

    @Test
    void identifiersIgnoreFieldsOutsideTheExportedMapping(@TempDir Path directory) throws SaveException, IOException {
        BibEntry first = new BibEntry(StandardEntryType.Book).withField(StandardField.TITLE, "A book");
        BibEntry second = new BibEntry(StandardEntryType.Book).withField(StandardField.TITLE, "A book")
                                                              .withField(StandardField.NOTE, "Local note").withField(StandardField.FILE, "local.pdf");
        Path firstFile = directory.resolve("first.rdf");
        Path secondFile = directory.resolve("second.rdf");

        exporter.export(new BibDatabaseContext(), firstFile, List.of(first));
        exporter.export(new BibDatabaseContext(), secondFile, List.of(second));

        assertEquals(Files.readString(firstFile), Files.readString(secondFile));
    }

    @Test
    void articleWithoutIssnRetainsJournalAndType(@TempDir Path directory) throws SaveException, IOException {
        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "An article")
                .withField(StandardField.JOURNAL, "A journal")
                .withField(StandardField.PAGES, "1-5");
        Path output = directory.resolve("article.rdf");

        exporter.export(new BibDatabaseContext(), output, List.of(article));
        BibEntry restored = importer.importDatabase(output).getDatabase().getEntries().getFirst();

        assertEquals(article, restored);
    }

    @Test
    void articleWithoutJournalRetainsIssn(@TempDir Path directory) throws SaveException, IOException {
        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "An article")
                .withField(StandardField.ISSN, "1234-5678");
        Path output = directory.resolve("article.rdf");

        exporter.export(new BibDatabaseContext(), output, List.of(article));
        BibEntry restored = importer.importDatabase(output).getDatabase().getEntries().getFirst();

        assertEquals(article, restored);
    }

    @Test
    void articleWithoutHostFieldsRetainsType(@TempDir Path directory) throws SaveException, IOException {
        BibEntry article = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "An article");
        Path output = directory.resolve("article.rdf");

        exporter.export(new BibDatabaseContext(), output, List.of(article));
        BibEntry restored = importer.importDatabase(output).getDatabase().getEntries().getFirst();

        assertEquals(article, restored);
    }

    @Test
    void publicationYearIsAlsoExportedAsStructuredEdtfDate(@TempDir Path directory) throws SaveException, IOException {
        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "A book")
                .withField(StandardField.YEAR, "2023");
        Path output = directory.resolve("book.rdf");

        exporter.export(new BibDatabaseContext(), output, List.of(book));

        assertTrue(Files.readString(output).contains(
                "<bf:date rdf:datatype=\"http://id.loc.gov/datatypes/edtf\">2023</bf:date>"));
    }

    @Test
    void languagesUseCodesOnlyForRecognizedSingleLanguages(@TempDir Path directory) throws SaveException, IOException {
        for (String language : List.of("English", "Deutsch", "English, Japanese")) {
            BibEntry book = new BibEntry(StandardEntryType.Book)
                    .withField(StandardField.TITLE, "A book")
                    .withField(StandardField.LANGUAGE, language);
            Path output = directory.resolve("language.rdf");

            exporter.export(new BibDatabaseContext(), output, List.of(book));
            String xml = Files.readString(output);
            BibEntry restored = importer.importDatabase(output).getDatabase().getEntries().getFirst();

            String expected = switch (language) {
                case "English" ->
                        "eng";
                case "Deutsch" ->
                        "ger";
                default ->
                        "English, Japanese";
            };
            assertEquals(book.withField(StandardField.LANGUAGE, expected), restored);
            assertFalse(xml.contains("/languages/English"));
            assertFalse(xml.contains("/languages/Deutsch"));
        }
    }

    @Test
    void escapesXmlAndKeepsTwoEntriesSeparate(@TempDir Path directory)
            throws SaveException, IOException, ParserConfigurationException, SAXException {
        BibEntry first = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "A < B & \"C\"");
        BibEntry second = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Second book");
        Path output = directory.resolve("multiple.rdf");

        exporter.export(new BibDatabaseContext(), output, List.of(first, second));

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(output.toFile());
        assertEquals(2, document.getElementsByTagName("bf:Work").getLength());
        assertEquals(2, document.getElementsByTagName("bf:Instance").getLength());
        for (int index = 0; index < 2; index++) {
            Element work = (Element) document.getElementsByTagName("bf:Work").item(index);
            Element instance = (Element) document.getElementsByTagName("bf:Instance").item(index);
            assertEquals(instance.getAttribute("rdf:about"),
                    ((Element) work.getElementsByTagName("bf:hasInstance").item(0)).getAttribute("rdf:resource"));
            assertEquals(work.getAttribute("rdf:about"),
                    ((Element) instance.getElementsByTagName("bf:instanceOf").item(0)).getAttribute("rdf:resource"));
        }
        assertNotEquals(((Element) document.getElementsByTagName("bf:Work").item(0)).getAttribute("rdf:about"),
                ((Element) document.getElementsByTagName("bf:Work").item(1)).getAttribute("rdf:about"));
        assertEquals(List.of(first, second), importer.importDatabase(output).getDatabase().getEntries());
        assertFalse(Files.readString(output).contains("first-key"));
    }

    @Test
    void emptyInputWritesEmptyFile(@TempDir Path directory) throws SaveException, IOException {
        Path output = directory.resolve("empty.rdf");

        exporter.export(new BibDatabaseContext(), output, List.of());

        assertEquals("", Files.readString(output));
    }
}
