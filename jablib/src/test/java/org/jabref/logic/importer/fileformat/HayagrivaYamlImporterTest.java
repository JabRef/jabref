package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HayagrivaYamlImporterTest {

    private HayagrivaYamlImporter importer;

    @BeforeEach
    void setUp() {
        importer = new HayagrivaYamlImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Hayagriva YAML", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("hayagrivayaml", importer.getId());
    }

    @Test
    void getExtensions() {
        assertEquals(StandardFileType.YAML, importer.getFileType());
    }

    @Test
    void getDescription() {
        assertEquals("Importer for the Hayagriva YAML format, which is used by the Typst typesetting system.",
                importer.getDescription());
    }

    @Test
    void isRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestValid.yml").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }

    @Test
    void isRecognizedFormatReject() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestInvalid.yml").toURI());
        assertFalse(importer.isRecognizedFormat(file));
    }

    @Test
    void importBasicArticle() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestValid.yml").toURI());
        BibEntry entry = importer.importDatabase(file).getDatabase().getEntries().getFirst();

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("zygos")
                .withField(StandardField.TITLE, "A Test Article")
                .withField(StandardField.AUTHOR, "Smith, John and Doe, Jane")
                .withField(StandardField.DATE, "2023-05-01")
                .withField(StandardField.PAGES, "1-10")
                .withField(StandardField.JOURNAL, "Journal of Testing")
                .withField(StandardField.VOLUME, "5")
                .withField(StandardField.NUMBER, "2");

        assertEquals(expected, entry);
    }

    @Test
    void importMultipleEntriesCount() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestMultipleEntries.yml").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(2, entries.size());
    }

    @Test
    void importMultipleEntriesArticle() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestMultipleEntries.yml").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry article = entries.stream()
                                  .filter(e -> "article-entry".equals(e.getCitationKey().orElse("")))
                                  .findFirst()
                                  .orElseThrow();

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("article-entry")
                .withField(StandardField.TITLE, "An Article Entry")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.DATE, "2021-03-15")
                .withField(StandardField.DOI, "10.1000/test")
                .withField(StandardField.JOURNAL, "Testing Journal")
                .withField(StandardField.VOLUME, "3")
                .withField(StandardField.NUMBER, "1");

        assertEquals(expected, article);
    }

    @Test
    void importMultipleEntriesBook() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestMultipleEntries.yml").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry book = entries.stream()
                               .filter(e -> "book-entry".equals(e.getCitationKey().orElse("")))
                               .findFirst()
                               .orElseThrow();

        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withCitationKey("book-entry")
                .withField(StandardField.TITLE, "A Book Entry")
                .withField(StandardField.AUTHOR, "Jones, Alice")
                .withField(StandardField.DATE, "2019")
                .withField(StandardField.ISBN, "978-3-16-148410-0")
                .withField(StandardField.PUBLISHER, "Test Publisher")
                .withField(StandardField.ADDRESS, "Berlin");

        assertEquals(expected, book);
    }

    @Test
    void importSerialNumberDoi() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestSerialNumber.yml").toURI());
        BibEntry entry = importer.importDatabase(file).getDatabase().getEntries().getFirst();

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("serial-entry")
                .withField(StandardField.TITLE, "Serial Number Test")
                .withField(StandardField.AUTHOR, "Brown, Bob")
                .withField(StandardField.DATE, "2022-11-01")
                .withField(StandardField.DOI, "10.1128/MBio.00033-11")
                .withField(StandardField.PMID, "12345678")
                .withField(StandardField.JOURNAL, "Serial Journal")
                .withField(StandardField.VOLUME, "2")
                .withField(StandardField.NUMBER, "4");

        assertEquals(expected, entry);
    }

    @Test
    void importBasicYmlKinetics() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestBasic.yml").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = entries.stream()
                                .filter(e -> "kinetics".equals(e.getCitationKey().orElse("")))
                                .findFirst()
                                .orElseThrow();

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("kinetics")
                .withField(StandardField.TITLE, "Kinetics and luminescence of the excitations of a nonequilibrium polariton condensate")
                .withField(StandardField.AUTHOR, "Doan, T. D. and Tran Thoai, D. B. and Haug, Hartmut")
                .withField(StandardField.DOI, "10.1103/PhysRevB.102.165126")
                .withField(StandardField.PAGES, "165126-165139")
                .withField(StandardField.DATE, "2020-10-14")
                .withField(StandardField.JOURNAL, "Physical Review B")
                .withField(StandardField.VOLUME, "102")
                .withField(StandardField.NUMBER, "16")
                .withField(StandardField.PUBLISHER, "American Physical Society")
                .withField(new org.jabref.model.entry.field.UnknownField("page-total"), "13");

        assertEquals(expected, entry);
    }

    @Test
    void importBasicYmlDonne() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestBasic.yml").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = entries.stream()
                                .filter(e -> "donne".equals(e.getCitationKey().orElse("")))
                                .findFirst()
                                .orElseThrow();

        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withCitationKey("donne")
                .withField(StandardField.TITLE, "The \"Anniversaries\" and the \"Epicedes and Obsequies\"")
                .withField(StandardField.AUTHOR, "Donne, John")
                .withField(StandardField.EDITOR, "Stringer, Gary A. and Pebworth, Ted-Larry")
                .withField(StandardField.ADDRESS, "Bloomington")
                .withField(StandardField.VOLUME, "6")
                .withField(StandardField.PUBLISHER, "Indiana University Press")
                .withField(StandardField.DATE, "1995")
                .withField(StandardField.BOOKTITLE, "The Variorum Edition of the Poetry of John Donne");

        assertEquals(expected, entry);
    }

    @Test
    void importBasicYmlHouse() throws IOException, URISyntaxException {
        Path file = Path.of(HayagrivaYamlImporterTest.class.getResource("HayagrivaYamlImporterTestBasic.yml").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        BibEntry entry = entries.stream()
                                .filter(e -> "house".equals(e.getCitationKey().orElse("")))
                                .findFirst()
                                .orElseThrow();

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("house")
                .withField(StandardField.TITLE, "Teaching medicine with the help of \"Dr. House\"")
                .withField(StandardField.AUTHOR, "Jerrentrup, Andreas and Mueller, Tobias and Glowalla, Ulrich and Herder, Meike and Henrichs, Nadine and Neubauer, Andreas and Schaefer, Juergen R.")
                .withField(StandardField.DOI, "10.1371/journal.pone.0193972")
                .withField(StandardField.DATE, "2018-03-13")
                .withField(StandardField.JOURNAL, "PLoS ONE")
                .withField(StandardField.VOLUME, "13")
                .withField(StandardField.NUMBER, "3");

        assertEquals(expected, entry);
    }
}
