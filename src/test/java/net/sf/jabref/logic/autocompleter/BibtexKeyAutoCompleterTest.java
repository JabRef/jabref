package net.sf.jabref.logic.autocompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        entry.setCiteKey("testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("testKey");
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeBeginnigOfKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeLowercaseKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("testkey");
        Assert.assertEquals(Arrays.asList("testKey"), result);
    }

    @Test
    public void completeNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete(null);
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setCiteKey("testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entryOne = new BibEntry();
        entryOne.setCiteKey("testKeyOne");
        autoCompleter.addBibtexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setCiteKey("testKeyTwo");
        autoCompleter.addBibtexEntry(entryTwo);

        List<String> result = autoCompleter.complete("testKey");
        Assert.assertEquals(Arrays.asList("testKeyOne", "testKeyTwo"), result);
    }

    @Test
    public void completeShortKeyReturnsKey() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        BibtexKeyAutoCompleter autoCompleter = new BibtexKeyAutoCompleter(preferences);

        BibEntry entry = new BibEntry();
        entry.setCiteKey("key");
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
        entry.setCiteKey("testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }
}
