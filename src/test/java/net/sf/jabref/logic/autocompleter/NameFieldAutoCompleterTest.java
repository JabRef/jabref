package net.sf.jabref.logic.autocompleter;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.sf.jabref.model.entry.BibEntry;

public class NameFieldAutoCompleterTest {

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullPreferenceThrowsException() {
        new NameFieldAutoCompleter("field", null);
    }

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullFieldThrowsException() {
        new NameFieldAutoCompleter(null, mock(AutoCompletePreferences.class));
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        autoCompleter.addBibtexEntry(null);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("title", "testTitle");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Testname");
        Assert.assertEquals(Arrays.asList("Testname"), result);
    }

    @Test
    public void completeBeginnigOfNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Test");
        Assert.assertEquals(Arrays.asList("Testname"), result);
    }

    @Test
    public void completeLowercaseNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Arrays.asList("Testname"), result);
    }

    @Test
    public void completeNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete(null);
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entryOne = new BibEntry();
        entryOne.setField("field", "testNameOne");
        autoCompleter.addBibtexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField("field", "testNameTwo");
        autoCompleter.addBibtexEntry(entryTwo);

        List<String> result = autoCompleter.complete("testName");
        Assert.assertEquals(Arrays.asList("testNameOne", "testNameTwo"), result);
    }

    @Test
    public void completeTooShortInputReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getShortestLengthToComplete()).thenReturn(100);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("test");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completePartOfNameReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("osta");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Vas");
        Assert.assertEquals(Arrays.asList("Vassilis Kostakos"), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteNameWithJr() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Reagle, Jr., Joseph M.");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Jos");
        Assert.assertEquals(Arrays.asList("Joseph M. Reagle, Jr."), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteNameWithVon() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Eric");
        Assert.assertEquals(Arrays.asList("Eric von Hippel"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithUmlauts() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_FULL);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Honig Bär");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Bä");
        Assert.assertEquals(Arrays.asList("Bär, Honig"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameAndNameWithInitialFirstname() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Kosta");
        Assert.assertEquals(Arrays.asList("Kostakos, V.", "Kostakos, Vassilis"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_FULL);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Kosta");
        Assert.assertEquals(Arrays.asList("Kostakos, Vassilis"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithJrIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Reagle, Jr., Joseph M.");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Rea");
        Assert.assertEquals(Arrays.asList("Reagle, Jr., J. M."), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithInitialFirstnameIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Kosta");
        Assert.assertEquals(Arrays.asList("Kostakos, V."), result);
    }

    @Test
    public void completeVonReturnsNameWithInitialFirstnameIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("von");
        Assert.assertEquals(Arrays.asList("von Hippel, E."), result);
    }

    @Test
    public void completeBeginningOfNameReturnsCompleteName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Kostakos, Va");
        Assert.assertEquals(Arrays.asList("Kostakos, Vassilis"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNothingIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getOnlyCompleteFirstLast()).thenReturn(true);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Kosta");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsNothingIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getOnlyCompleteLastFirst()).thenReturn(true);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("Vas");
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeShortNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibEntry entry = new BibEntry();
        entry.setField("field", "nam");
        autoCompleter.addBibtexEntry(entry);

        List<String> result = autoCompleter.complete("n");
        Assert.assertEquals(Arrays.asList("nam"), result);
    }
}
