package org.jabref.model.citation;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceEntryTest {

    @Test
    void basicCreation() {
        ReferenceEntry entry = new ReferenceEntry(
                "[1] Smith, J. (2020). Test Title. Journal of Testing, 10, 1-10.",
                "[1]"
        );

        assertEquals("[1]", entry.marker());
        assertTrue(entry.authors().isEmpty());
        assertTrue(entry.title().isEmpty());
    }

    @Test
    void creationWithMetadata() {
        ReferenceEntry entry = new ReferenceEntry(
                "[1] Smith, J. (2020). Test Title.",
                "[1]",
                "Smith, J.",
                "Test Title",
                "2020"
        );

        assertEquals("[1]", entry.marker());
        assertEquals(Optional.of("Smith, J."), entry.authors());
        assertEquals(Optional.of("Test Title"), entry.title());
        assertEquals(Optional.of("2020"), entry.year());
    }

    @Test
    void builderPattern() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] Full reference text", "[1]")
                                             .authors("Smith, John and Jones, Jane")
                                             .title("A Great Paper")
                                             .year("2020")
                                             .journal("Nature")
                                             .volume("10")
                                             .pages("1-15")
                                             .doi("10.1234/example")
                                             .url("https://example.com")
                                             .build();

        assertEquals("[1]", entry.marker());
        assertEquals(Optional.of("Smith, John and Jones, Jane"), entry.authors());
        assertEquals(Optional.of("A Great Paper"), entry.title());
        assertEquals(Optional.of("2020"), entry.year());
        assertEquals(Optional.of("Nature"), entry.journal());
        assertEquals(Optional.of("10"), entry.volume());
        assertEquals(Optional.of("1-15"), entry.pages());
        assertEquals(Optional.of("10.1234/example"), entry.doi());
        assertEquals(Optional.of("https://example.com"), entry.url());
    }

    @Test
    void toBibEntry() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] Reference text", "[1]")
                                             .authors("Smith, John")
                                             .title("Test Paper")
                                             .year("2020")
                                             .journal("Test Journal")
                                             .build();

        BibEntry bibEntry = entry.toBibEntry();

        assertEquals(StandardEntryType.Article, bibEntry.getType());
        assertEquals(Optional.of("Smith, John"), bibEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Test Paper"), bibEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2020"), bibEntry.getField(StandardField.YEAR));
        assertEquals(Optional.of("Test Journal"), bibEntry.getField(StandardField.JOURNAL));
    }

    @Test
    void toBibEntryWithoutJournal() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] Reference text", "[1]")
                                             .authors("Smith, John")
                                             .title("Test Paper")
                                             .year("2020")
                                             .build();

        BibEntry bibEntry = entry.toBibEntry();

        assertEquals(StandardEntryType.Misc, bibEntry.getType());
    }

    @Test
    void toBibEntryWithUrl() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] Reference text", "[1]")
                                             .title("Online Resource")
                                             .url("https://example.com")
                                             .build();

        BibEntry bibEntry = entry.toBibEntry();

        assertEquals(StandardEntryType.Online, bibEntry.getType());
    }

    @Test
    void generateCitationKey() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] Reference text", "[1]")
                                             .authors("Smith, John")
                                             .year("2020")
                                             .build();

        Optional<String> key = entry.generateCitationKey();

        assertTrue(key.isPresent());
        assertEquals("Smith2020", key.get());
    }

    @Test
    void generateCitationKeyFromMarker() {
        ReferenceEntry entry = new ReferenceEntry(
                "(Jones 2019) Reference text",
                "(Jones 2019)"
        );

        Optional<String> key = entry.generateCitationKey();

        assertTrue(key.isPresent());
        assertEquals("Jones2019", key.get());
    }

    @Test
    void getNormalizedMarker() {
        ReferenceEntry entry1 = new ReferenceEntry("text", "[1]");
        assertEquals("1", entry1.getNormalizedMarker());

        ReferenceEntry entry2 = new ReferenceEntry("text", "(Smith 2020)");
        assertEquals("Smith 2020", entry2.getNormalizedMarker());

        ReferenceEntry entry3 = new ReferenceEntry("text", "[Smith20]");
        assertEquals("Smith20", entry3.getNormalizedMarker());
    }

    @Test
    void getSearchQuery() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] Reference text", "[1]")
                                             .title("Machine Learning Applications")
                                             .authors("Smith, John")
                                             .year("2020")
                                             .build();

        String query = entry.getSearchQuery();

        assertTrue(query.contains("Machine Learning Applications"));
        assertTrue(query.contains("Smith"));
        assertTrue(query.contains("2020"));
    }

    @Test
    void getSearchQueryWithDoi() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] Reference text", "[1]")
                                             .doi("10.1234/example")
                                             .title("Some Title")
                                             .build();

        String query = entry.getSearchQuery();

        assertEquals("10.1234/example", query);
    }

    @Test
    void hasMinimalMetadata() {
        ReferenceEntry withTitle = ReferenceEntry.builder("[1] text", "[1]")
                                                 .title("A Title")
                                                 .build();
        assertTrue(withTitle.hasMinimalMetadata());

        ReferenceEntry withAuthorYear = ReferenceEntry.builder("[1] text", "[1]")
                                                      .authors("Smith")
                                                      .year("2020")
                                                      .build();
        assertTrue(withAuthorYear.hasMinimalMetadata());

        ReferenceEntry withNothing = new ReferenceEntry("[1] text", "[1]");
        assertFalse(withNothing.hasMinimalMetadata());
    }

    @Test
    void hasDoi() {
        ReferenceEntry withDoi = ReferenceEntry.builder("[1] text", "[1]")
                                               .doi("10.1234/example")
                                               .build();
        assertTrue(withDoi.hasDoi());

        ReferenceEntry withoutDoi = new ReferenceEntry("[1] text", "[1]");
        assertFalse(withoutDoi.hasDoi());
    }

    @Test
    void nullRawTextThrows() {
        assertThrows(NullPointerException.class, () ->
                new ReferenceEntry(null, "[1]"));
    }

    @Test
    void blankRawTextThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReferenceEntry("   ", "[1]"));
    }

    @Test
    void nullMarkerThrows() {
        assertThrows(NullPointerException.class, () ->
                new ReferenceEntry("Some text", null));
    }

    @Test
    void blankMarkerThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReferenceEntry("Some text", "  "));
    }

    @Test
    void blankFieldsAreFilteredOut() {
        ReferenceEntry entry = ReferenceEntry.builder("[1] text", "[1]")
                                             .authors("  ")
                                             .title("")
                                             .year("2020")
                                             .build();

        assertTrue(entry.authors().isEmpty());
        assertTrue(entry.title().isEmpty());
        assertEquals(Optional.of("2020"), entry.year());
    }
}
