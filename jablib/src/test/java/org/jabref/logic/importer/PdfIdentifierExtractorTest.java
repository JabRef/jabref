package org.jabref.logic.importer;

import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfIdentifierExtractorTest {

    @Test
    void extractIdentifiersReturnsEmptyMapForEmptyList() {
        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of());
        assertEquals(Map.of(), result);
    }

    @Test
    void extractIdentifiersReturnsEmptyMapWhenNoIdentifiersPresent() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.AUTHOR, "Author Name");

        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(entry));
        assertEquals(Map.of(), result);
    }

    @Test
    void extractIdentifiersFindsDoiFromSingleEntry() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DOI, "10.1145/3651640.3651646");
        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(entry));
        assertEquals(Map.of(StandardField.DOI, "10.1145/3651640.3651646"), result);
    }

    @Test
    void extractIdentifiersFindsIsbnFromSingleEntry() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.ISBN, "978-0-13-468599-1");
        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(entry));
        assertEquals(Map.of(StandardField.ISBN, "978-0-13-468599-1"), result);
    }

    @Test
    void extractIdentifiersFindsMultipleIdentifiers() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.DOI, "10.1234/test")
                .withField(StandardField.ISBN, "978-3-16-148410-0")
                .withField(StandardField.EPRINT, "2408.06224");
        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(entry));
        assertEquals(Map.of(
                        StandardField.DOI, "10.1234/test",
                        StandardField.ISBN, "978-3-16-148410-0",
                        StandardField.EPRINT, "2408.06224"),
                result);
    }

    @Test
    void extractIdentifiersFirstEntryValueWinsForSameField() {
        BibEntry first = new BibEntry()
                .withField(StandardField.DOI, "10.1111/first");
        BibEntry second = new BibEntry()
                .withField(StandardField.DOI, "10.2222/second");
        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(first, second));
        assertEquals("10.1111/first", result.get(StandardField.DOI));
    }

    @Test
    void extractIdentifiersMergesAcrossEntries() {
        BibEntry first = new BibEntry()
                .withField(StandardField.DOI, "10.1234/test");
        BibEntry second = new BibEntry()
                .withField(StandardField.ISBN, "978-3-16-148410-0");
        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(first, second));
        assertEquals(Map.of(
                        StandardField.DOI, "10.1234/test",
                        StandardField.ISBN, "978-3-16-148410-0"),
                result);
    }

    @Test
    void extractIdentifiersIgnoresNonIdentifierFields() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.AUTHOR, "Author")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.DOI, "10.1234/test");
        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(entry));
        assertEquals(1, result.size());
        assertEquals("10.1234/test", result.get(StandardField.DOI));
    }

    @Test
    void extractIdentifiersSkipsEmptyValuesAndUsesNextValid() {
        BibEntry first = new BibEntry()
                .withField(StandardField.DOI, ""); // empty

        BibEntry second = new BibEntry()
                .withField(StandardField.DOI, "10.1234/test");

        Map<Field, String> result = PdfIdentifierExtractor.extractIdentifiers(List.of(first, second));

        assertEquals(Map.of(StandardField.DOI, "10.1234/test"), result);
    }
}
