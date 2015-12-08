package net.sf.jabref.logic.autocompleter;

import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Test;

import net.sf.jabref.model.entry.BibtexEntry;

public class NameFieldAutoCompleterTest {

    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullPreferenceThrowsException() {
        new NameFieldAutoCompleter("field", null);
    }

    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullFieldThrowsException() {
        new NameFieldAutoCompleter(null, mock(AutoCompletePreferences.class));
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        autoCompleter.addBibtexEntry(null);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("title", "testTitle");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Testname");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Testname", result[0]);
    }

    @Test
    public void completeBeginnigOfNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Test");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Testname", result[0]);
    }

    @Test
    public void completeLowercaseNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Testname", result[0]);
    }

    @Test
    public void completeNullReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete(null);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "testKey");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeReturnsMultipleResults() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entryOne = new BibtexEntry();
        entryOne.setField("field", "testNameOne");
        autoCompleter.addBibtexEntry(entryOne);
        BibtexEntry entryTwo = new BibtexEntry();
        entryTwo.setField("field", "testNameTwo");
        autoCompleter.addBibtexEntry(entryTwo);

        String[] result = autoCompleter.complete("testName");
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("testNameOne", result[0]);
        Assert.assertEquals("testNameTwo", result[1]);
    }

    @Test
    public void completeTooShortInputReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getShortestLengthToComplete()).thenReturn(100);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Testname");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("test");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completePartOfNameReturnsNothing() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("osta");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Vas");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Vassilis Kostakos", result[0]);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteNameWithJr() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Reagle, Jr., Joseph M.");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Jos");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Joseph M. Reagle, Jr.", result[0]);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteNameWithVon() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Eric");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Eric von Hippel", result[0]);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithUmlauts() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_FULL);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Honig Bär");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Bä");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Bär, Honig", result[0]);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameAndNameWithInitialFirstname() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Kosta");
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("Kostakos, V.", result[0]);
        Assert.assertEquals("Kostakos, Vassilis", result[1]);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_FULL);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Kosta");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Kostakos, Vassilis", result[0]);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithJrIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Reagle, Jr., Joseph M.");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Rea");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Reagle, Jr., J. M.", result[0]);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithInitialFirstnameIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Kosta");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Kostakos, V.", result[0]);
    }

    @Test
    public void completeVonReturnsNameWithInitialFirstnameIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("von");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("von Hippel, E.", result[0]);
    }

    @Test
    public void completeBeginningOfNameReturnsCompleteName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Kostakos, Va");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("Kostakos, Vassilis", result[0]);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNothingIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getOnlyCompleteFirstLast()).thenReturn(true);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Kosta");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsNothingIfPref() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        when(preferences.getOnlyCompleteLastFirst()).thenReturn(true);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("Vas");
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void completeShortNameReturnsName() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        NameFieldAutoCompleter autoCompleter = new NameFieldAutoCompleter("field", preferences);

        BibtexEntry entry = new BibtexEntry();
        entry.setField("field", "nam");
        autoCompleter.addBibtexEntry(entry);
        String[] result = autoCompleter.complete("n");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("nam", result[0]);
    }
}
