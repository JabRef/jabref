package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BibEntrySuggestionProviderTest {

    private BibEntrySuggestionProvider autoCompleter;

    @BeforeEach
    public void setUp() throws Exception {
        autoCompleter = new BibEntrySuggestionProvider();
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        autoCompleter.indexEntry(null);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("testKey")));
        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    public void completeBeginnigOfKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    public void completeLowercaseKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("testkey")));
        assertEquals(Collections.singletonList(entry), result);
    }

    @Test
    public void completeNullThrowsException() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        assertThrows(NullPointerException.class, () -> autoCompleter.call(getRequest((null))));
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        BibEntry entryOne = new BibEntry();
        entryOne.setCiteKey("testKeyOne");
        autoCompleter.indexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setCiteKey("testKeyTwo");
        autoCompleter.indexEntry(entryTwo);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("testKey")));
        assertEquals(Arrays.asList(entryTwo, entryOne), result);
    }

    @Test
    public void completeShortKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("key");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("k")));
        assertEquals(Collections.singletonList(entry), result);
    }
}
