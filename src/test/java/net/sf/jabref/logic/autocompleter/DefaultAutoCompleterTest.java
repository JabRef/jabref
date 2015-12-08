package net.sf.jabref.logic.autocompleter;

import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Test;

import net.sf.jabref.model.entry.BibtexEntry;

public class DefaultAutoCompleterTest {

    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullPreferenceThrowsException() {
        new DefaultAutoCompleter("field", null);
    }

    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullFieldThrowsException() {
        new DefaultAutoCompleter(null, mock(AutoCompletePreferences.class));
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        autoCompleter.addBibtexEntry(null);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("title", "testTitle");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeValueReturnsValue() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("testValue");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("testValue", result[0]);
    }

    @Test
    public void completeBeginnigOfValueReturnsValue() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("testValue", result[0]);
    }

    @Test
    public void completeLowercaseValueReturnsValue() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("testvalue");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("testValue", result[0]);
    }

    @Test
    public void completeNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete(null);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeReturnsMultipleResults() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entryOne = new BibtexEntry();
        entryOne.setField("field", "testValueOne");
        autoCompleter.addBibtexEntry(entryOne);
        BibtexEntry entryTwo = new BibtexEntry();
        entryTwo.setField("field", "testValueTwo");
        autoCompleter.addBibtexEntry(entryTwo);

        String[] result = autoCompleter.complete("testValue");
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("testValueOne", result[0]);
        Assert.assertEquals("testValueTwo", result[1]);
    }

    @Test
    public void completeShortStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "val");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("va");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeTooShortInputReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getShortestLengthToComplete()).thenReturn(100);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testValue");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeBeginnigOfSecondWordReturnsWord() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "test value");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("val");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("value", result[0]);
    }

    @Test
    public void completePartOfWordReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        DefaultAutoCompleter autoCompleter = new DefaultAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "test value");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("lue");
        Assert.assertEquals(0, result.length);
    }
}
