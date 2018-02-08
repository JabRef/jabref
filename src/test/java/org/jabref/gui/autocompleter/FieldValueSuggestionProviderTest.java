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

public class FieldValueSuggestionProviderTest {

    private FieldValueSuggestionProvider autoCompleter;

    @BeforeEach
    public void setUp() throws Exception {
        autoCompleter = new FieldValueSuggestionProvider("field");
    }

    @Test
    public void initAutoCompleterWithNullFieldThrowsException() {
        assertThrows(NullPointerException.class, () -> new FieldValueSuggestionProvider(null));
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        Collection<String> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        autoCompleter.indexEntry(null);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "testTitle");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("testValue")));
        assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeBeginnigOfValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeLowercaseValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("testvalue")));
        assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeNullThrowsException() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.indexEntry(entry);

        assertThrows(NullPointerException.class, () -> autoCompleter.call(getRequest(null)));
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        BibEntry entryOne = new BibEntry();
        entryOne.setField("field", "testValueOne");
        autoCompleter.indexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField("field", "testValueTwo");
        autoCompleter.indexEntry(entryTwo);

        Collection<String> result = autoCompleter.call(getRequest(("testValue")));
        assertEquals(Arrays.asList("testValueOne", "testValueTwo"), result);
    }

    @Test
    public void completeShortStringReturnsFieldValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "val");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("va")));
        assertEquals(Collections.singletonList("val"), result);
    }

    @Test
    public void completeBeginnigOfSecondWordReturnsWholeFieldValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("val")));
        assertEquals(Collections.singletonList("test value"), result);
    }

    @Test
    public void completePartOfWordReturnsWholeFieldValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("lue")));
        assertEquals(Collections.singletonList("test value"), result);
    }

    @Test
    public void completeReturnsWholeFieldValue() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.indexEntry(entry);

        Collection<String> result = autoCompleter.call(getRequest(("te")));
        assertEquals(Collections.singletonList("test value"), result);
    }
}
