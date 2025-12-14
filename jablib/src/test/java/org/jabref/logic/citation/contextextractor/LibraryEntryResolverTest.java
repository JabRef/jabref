package org.jabref.logic.citation.contextextractor;

import java.util.Optional;

import org.jabref.model.citation.ReferenceEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryEntryResolverTest {

    private BibDatabase database;
    private LibraryEntryResolver resolver;
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        database = new BibDatabase();
        entryTypesManager = new BibEntryTypesManager();
        resolver = new LibraryEntryResolver(database, BibDatabaseMode.BIBTEX, entryTypesManager);
    }

    @Test
    void testResolveReferenceCreatesNewEntry() {
        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .authors("Smith, John")
                .title("New Paper")
                .year("2020")
                .build();

        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveReference(reference);

        assertTrue(resolved.isNew());
        assertNotNull(resolved.entry());
        assertEquals(LibraryEntryResolver.MatchType.NEW_ENTRY, resolved.matchType());
    }

    @Test
    void testResolveReferenceFindsExistingByDoi() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.DOI, "10.1234/existing");
        existingEntry.setField(StandardField.TITLE, "Existing Paper");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .doi("10.1234/existing")
                .build();

        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveReference(reference);

        assertFalse(resolved.isNew());
        assertEquals(existingEntry, resolved.entry());
        assertEquals(LibraryEntryResolver.MatchType.DOI, resolved.matchType());
        assertTrue(resolved.isHighConfidence());
    }

    @Test
    void testResolveReferenceFindsExistingByCitationKey() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setCitationKey("Smith2020");
        existingEntry.setField(StandardField.AUTHOR, "Smith, John");
        existingEntry.setField(StandardField.YEAR, "2020");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .authors("Smith, John")
                .year("2020")
                .build();

        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveReference(reference);

        assertFalse(resolved.isNew());
        assertEquals(LibraryEntryResolver.MatchType.CITATION_KEY, resolved.matchType());
    }

    @Test
    void testResolveReferenceFindsExistingByTitle() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.TITLE, "Machine Learning Applications in Healthcare");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .title("Machine Learning Applications in Healthcare")
                .build();

        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveReference(reference);

        assertFalse(resolved.isNew());
        assertEquals(LibraryEntryResolver.MatchType.TITLE, resolved.matchType());
    }

    @Test
    void testResolveReferenceFindsExistingByAuthorAndYear() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.AUTHOR, "Johnson, Alice");
        existingEntry.setField(StandardField.YEAR, "2019");
        existingEntry.setField(StandardField.TITLE, "Unique Title Not In Reference");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .authors("Johnson, A.")
                .year("2019")
                .title("Different Title That Does Not Match")
                .build();

        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveReference(reference);

        assertFalse(resolved.isNew());
        assertEquals(LibraryEntryResolver.MatchType.AUTHOR_YEAR, resolved.matchType());
    }

    @Test
    void testEntryExists() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.DOI, "10.1234/test");
        database.insertEntry(existingEntry);

        ReferenceEntry existingRef = ReferenceEntry.builder("[1] text", "[1]")
                .doi("10.1234/test")
                .build();

        ReferenceEntry newRef = ReferenceEntry.builder("[2] text", "[2]")
                .doi("10.5678/new")
                .build();

        assertTrue(resolver.entryExists(existingRef));
        assertFalse(resolver.entryExists(newRef));
    }

    @Test
    void testAddEntryIfNotExists() {
        BibEntry newEntry = new BibEntry(StandardEntryType.Article);
        newEntry.setCitationKey("NewEntry2024");
        newEntry.setField(StandardField.TITLE, "Brand New Paper");

        boolean added = resolver.addEntryIfNotExists(newEntry);

        assertTrue(added);
        assertTrue(database.getEntryByCitationKey("NewEntry2024").isPresent());
    }

    @Test
    void testAddEntryIfNotExistsReturnsFalseForDuplicate() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setCitationKey("Existing2024");
        database.insertEntry(existingEntry);

        BibEntry duplicateEntry = new BibEntry(StandardEntryType.Article);
        duplicateEntry.setCitationKey("Existing2024");

        boolean added = resolver.addEntryIfNotExists(duplicateEntry);

        assertFalse(added);
    }

    @Test
    void testResolveAndAdd() {
        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .authors("NewAuthor, Test")
                .title("Completely New Paper")
                .year("2024")
                .build();

        int initialSize = database.getEntries().size();
        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveAndAdd(reference);

        assertTrue(resolved.isNew());
        assertEquals(initialSize + 1, database.getEntries().size());
    }

    @Test
    void testResolveAndAddDoesNotDuplicateExisting() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.DOI, "10.1234/existing");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .doi("10.1234/existing")
                .build();

        int initialSize = database.getEntries().size();
        resolver.resolveAndAdd(reference);

        assertEquals(initialSize, database.getEntries().size());
    }

    @Test
    void testFindExistingEntry() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.DOI, "10.1234/findme");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .doi("10.1234/findme")
                .build();

        Optional<LibraryEntryResolver.MatchedEntry> found = resolver.findExistingEntry(reference);

        assertTrue(found.isPresent());
        assertEquals(existingEntry, found.get().entry());
    }

    @Test
    void testCreateEntryFromReference() {
        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .authors("Test, Author")
                .title("Test Title")
                .year("2020")
                .journal("Test Journal")
                .doi("10.1234/test")
                .build();

        BibEntry created = resolver.createEntryFromReference(reference);

        assertEquals(Optional.of("Test, Author"), created.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Test Title"), created.getField(StandardField.TITLE));
        assertEquals(Optional.of("2020"), created.getField(StandardField.YEAR));
        assertEquals(Optional.of("Test Journal"), created.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("10.1234/test"), created.getField(StandardField.DOI));
    }

    @Test
    void testResolvedEntryConfidenceLevels() {
        LibraryEntryResolver.ResolvedEntry high = new LibraryEntryResolver.ResolvedEntry(
                new BibEntry(), false, 0.9, LibraryEntryResolver.MatchType.DOI);
        assertTrue(high.isHighConfidence());
        assertFalse(high.isMediumConfidence());
        assertFalse(high.isLowConfidence());

        LibraryEntryResolver.ResolvedEntry medium = new LibraryEntryResolver.ResolvedEntry(
                new BibEntry(), false, 0.6, LibraryEntryResolver.MatchType.TITLE);
        assertFalse(medium.isHighConfidence());
        assertTrue(medium.isMediumConfidence());
        assertFalse(medium.isLowConfidence());

        LibraryEntryResolver.ResolvedEntry low = new LibraryEntryResolver.ResolvedEntry(
                new BibEntry(), false, 0.3, LibraryEntryResolver.MatchType.AUTHOR_YEAR);
        assertFalse(low.isHighConfidence());
        assertFalse(low.isMediumConfidence());
        assertTrue(low.isLowConfidence());
    }

    @Test
    void testNullDatabaseThrows() {
        assertThrows(NullPointerException.class, () ->
                new LibraryEntryResolver(null, BibDatabaseMode.BIBTEX, entryTypesManager));
    }

    @Test
    void testNullDatabaseModeThrows() {
        assertThrows(NullPointerException.class, () ->
                new LibraryEntryResolver(database, null, entryTypesManager));
    }

    @Test
    void testNullEntryTypesManagerThrows() {
        assertThrows(NullPointerException.class, () ->
                new LibraryEntryResolver(database, BibDatabaseMode.BIBTEX, null));
    }

    @Test
    void testNullReferenceThrows() {
        assertThrows(NullPointerException.class, () ->
                resolver.resolveReference(null));
    }

    @Test
    void testSimilarTitleMatch() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.TITLE, "Deep Learning for Natural Language Processing");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .title("Deep Learning for Natural Language Processing: A Survey")
                .build();

        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveReference(reference);

        assertNotNull(resolved);
    }

    @Test
    void testDoiCaseInsensitiveMatch() {
        BibEntry existingEntry = new BibEntry(StandardEntryType.Article);
        existingEntry.setField(StandardField.DOI, "10.1234/UPPERCASE");
        database.insertEntry(existingEntry);

        ReferenceEntry reference = ReferenceEntry.builder("[1] text", "[1]")
                .doi("10.1234/uppercase")
                .build();

        LibraryEntryResolver.ResolvedEntry resolved = resolver.resolveReference(reference);

        assertFalse(resolved.isNew());
        assertEquals(LibraryEntryResolver.MatchType.DOI, resolved.matchType());
    }
}
