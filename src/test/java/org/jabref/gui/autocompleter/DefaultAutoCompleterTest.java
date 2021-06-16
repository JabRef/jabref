package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAutoCompleterTest {

    private WordSuggestionProvider autoCompleter;
    private BibDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        database = new BibDatabase();
        autoCompleter = new WordSuggestionProvider(StandardField.TITLE, database);
    }

    @Test
    void initAutoCompleterWithNullFieldThrowsException() {
        assertThrows(NullPointerException.class, () -> new WordSuggestionProvider(null, database));
    }

    @Test
    void completeWithoutAddingAnythingReturnsNothing() {
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "testAuthor");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "testValue");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("testValue")));
        assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    void completeBeginningOfValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "testValue");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    void completeLowercaseValueReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "testValue");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("testvalue")));
        assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    void completeNullThrowsException() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "testKey");
        database.insertEntry(entry);

        assertThrows(NullPointerException.class, () -> autoCompleter.provideSuggestions(getRequest((null))));
    }

    @Test
    void completeEmptyStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "testKey");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeReturnsMultipleResults() {
        BibEntry entryOne = new BibEntry();
        entryOne.setField(StandardField.TITLE, "testValueOne");
        database.insertEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField(StandardField.TITLE, "testValueTwo");
        database.insertEntry(entryTwo);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("testValue")));
        assertEquals(Arrays.asList("testValueOne", "testValueTwo"), result);
    }

    @Test
    void completeShortStringReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "val");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("va")));
        assertEquals(Collections.singletonList("val"), result);
    }

    @Test
    void completeBeginnigOfSecondWordReturnsWord() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "test value");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("val")));
        assertEquals(Collections.singletonList("value"), result);
    }

    @Test
    void completePartOfWordReturnsValue() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "test value");
        database.insertEntry(entry);

        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("lue")));
        assertEquals(Collections.singletonList("value"), result);
    }
}
