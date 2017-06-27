package org.jabref.logic.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.jabref.logic.autocompleter.AutoCompleterTestUtil.getRequest;

public class FieldValueSuggestionProviderTest {
    private FieldValueSuggestionProvider autoCompleter;

    @Before
    public void setUp() throws Exception {
        autoCompleter = new FieldValueSuggestionProvider("field");
    }

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullFieldThrowsException() {
        new FieldValueSuggestionProvider(null);
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        Collection<String> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        autoCompleter.indexBibtexEntry(null);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "testTitle");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("testValue")));
        Assert.assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeBeginnigOfValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeLowercaseValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("testvalue")));
        Assert.assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeNullReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest((null)));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        BibEntry entryOne = new BibEntry();
        entryOne.setField("field", "testValueOne");
        autoCompleter.indexBibtexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField("field", "testValueTwo");
        autoCompleter.indexBibtexEntry(entryTwo);

        Collection<String> result = autoCompleter.call(getRequest(("testValue")));
        Assert.assertEquals(Arrays.asList("testValueOne", "testValueTwo"), result);
    }

    @Test
    public void completeShortStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "val");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("va")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeBeginnigOfSecondWordReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("val")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completePartOfWordReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("lue")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsWholeFieldValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.indexBibtexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("te")));
        Assert.assertEquals(Arrays.asList("test value"), result);
    }
}
