package net.sf.jabref.logic.autocompleter;

import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Test;

import net.sf.jabref.model.entry.BibtexEntry;

public class BibtexKeyAutoCompleterTest {

    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullPreferenceThrowsException() {
        new BibtexKeyAutoCompleter(null);
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        autoCompleter.addBibtexEntry(null);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField(BibtexEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("testKey");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("testKey", result[0]);
    }

    @Test
    public void completeBeginnigOfKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField(BibtexEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("testKey", result[0]);
    }

    @Test
    public void completeLowercaseKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField(BibtexEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("testkey");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("testKey", result[0]);
    }

    @Test
    public void completeNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField(BibtexEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete(null);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField(BibtexEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeReturnsMultipleResults() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entryOne = new BibtexEntry();
        entryOne.setField(BibtexEntry.KEY_FIELD, "testKeyOne");
        autoCompleter.addBibtexEntry(entryOne);
        BibtexEntry entryTwo = new BibtexEntry();
        entryTwo.setField(BibtexEntry.KEY_FIELD, "testKeyTwo");
        autoCompleter.addBibtexEntry(entryTwo);

        String[] result = autoCompleter.complete("testKey");
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("testKeyOne", result[0]);
        Assert.assertEquals("testKeyTwo", result[1]);
    }

    @Test
    public void completeShortKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField(BibtexEntry.KEY_FIELD, "key");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("k");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("key", result[0]);
    }

    @Test
    public void completeTooShortInputReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getShortestLengthToComplete()).thenReturn(100);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField(BibtexEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }
}
