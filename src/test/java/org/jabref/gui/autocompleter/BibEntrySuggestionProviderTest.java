package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BibEntrySuggestionProviderTest {

    private BibEntrySuggestionProvider autoCompleter;
    private BibDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        database = new BibDatabase();
        autoCompleter = new BibEntrySuggestionProvider(database);
    }

    @Test
    void completeWithoutAddingAnythingReturnsNothing() {
        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("testKey");
        database.insertEntry(entry);

        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("testKey")));
        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    void completeBeginningOfKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("testKey");
        database.insertEntry(entry);

        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    void completeLowercaseKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("testKey");
        database.insertEntry(entry);

        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("testkey")));
        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    void completeNullThrowsException() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("testKey");
        database.insertEntry(entry);

        assertThrows(NullPointerException.class, () -> autoCompleter.provideSuggestions(getRequest((null))));
    }

    @Test
    void completeEmptyStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("testKey");
        database.insertEntry(entry);

        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeReturnsMultipleResults() {
        BibEntry entryOne = new BibEntry();
        entryOne.setCitationKey("testKeyOne");
        database.insertEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setCitationKey("testKeyTwo");
        database.insertEntry(entryTwo);

        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("testKey")));
        assertEquals(Arrays.asList(entryTwo, entryOne), result);
    }

    @Test
    void completeShortKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCitationKey("key");
        database.insertEntry(entry);

        Collection<BibEntry> result = autoCompleter.provideSuggestions(getRequest(("k")));
        assertEquals(Collections.singletonList(entry), result);
    }
}
