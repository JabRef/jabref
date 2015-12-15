package net.sf.jabref.logic.autocompleter;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.sf.jabref.model.entry.BibEntry;

public class BibtexKeyAutoCompleterTest {

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullPreferenceThrowsException() {
        new BibtexKeyAutoCompleter(null);
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        autoCompleter.addBibtexEntry(null);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("testKey");
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeBeginnigOfKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeLowercaseKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("testkey");
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete(null);
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entryOne = new BibEntry();
        entryOne.setField(BibEntry.KEY_FIELD, "testKeyOne");
        autoCompleter.addBibtexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField(BibEntry.KEY_FIELD, "testKeyTwo");
        autoCompleter.addBibtexEntry(entryTwo);

        List<String> result = autoCompleter.complete("testKey");
        Assert.assertEquals(Arrays.asList("testKeyOne", "testKeyTwo"), result);
    }

    @Test
    public void completeShortKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "key");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("k");
        Assert.assertEquals(Arrays.asList("key"), result);
    }

    @Test
    public void completeTooShortInputReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getShortestLengthToComplete()).thenReturn(100);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }
}
