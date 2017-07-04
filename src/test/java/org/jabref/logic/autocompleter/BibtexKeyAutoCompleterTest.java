package org.jabref.logic.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;

public class BibtexKeyAutoCompleterTest {
    private BibEntrySuggestionProvider autoCompleter;

    @Before
    public void setUp() throws Exception {
        autoCompleter = new BibEntrySuggestionProvider();
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        autoCompleter.indexEntry(null);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("testKey")));
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeBeginnigOfKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeLowercaseKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("testkey")));
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeNullReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest((null)));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("")));
        Assert.assertEquals(Collections.emptyList(), result);
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
        Assert.assertEquals(Arrays.asList("testKeyOne", "testKeyTwo"), result);
    }

    @Test
    public void completeShortKeyReturnsKey() {
        BibEntry entry = new BibEntry();
        entry.setCiteKey("key");
        autoCompleter.indexEntry(entry);

        Collection<BibEntry> result = autoCompleter.call(getRequest(("k")));
        Assert.assertEquals(Arrays.asList("key"), result);
    }
}
