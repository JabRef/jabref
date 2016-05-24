package net.sf.jabref.logic.autocompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultAutoCompleterTest {

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullPreferenceThrowsException() {
        new DefaultAutoCompleter("field", null);
    }

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullFieldThrowsException() {
        new DefaultAutoCompleter(null, mock(AutoCompletePreferences.class));
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        autoCompleter.addBibtexEntry(null);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("title", "testTitle");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeValueReturnsValue() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("testValue");
        Assert.assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeBeginnigOfValueReturnsValue() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeLowercaseValueReturnsValue() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("testvalue");
        Assert.assertEquals(Arrays.asList("testValue"), result);
    }

    @Test
    public void completeNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete(null);
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entryOne = new BibEntry();
        entryOne.setField("field", "testValueOne");
        autoCompleter.addBibtexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField("field", "testValueTwo");
        autoCompleter.addBibtexEntry(entryTwo);

        List<String> result = autoCompleter.complete("testValue");
        Assert.assertEquals(Arrays.asList("testValueOne", "testValueTwo"), result);
    }

    @Test
    public void completeShortStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "val");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("va");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeTooShortInputReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getShortestLengthToComplete()).thenReturn(100);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeBeginnigOfSecondWordReturnsWord() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("val");
        Assert.assertEquals(Arrays.asList("value"), result);
    }

    @Test
    public void completePartOfWordReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "test value");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("lue");
        Assert.assertEquals(Collections.emptyList(), result);
    }
}
