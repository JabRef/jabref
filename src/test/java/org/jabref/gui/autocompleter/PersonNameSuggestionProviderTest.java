package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;

public class PersonNameSuggestionProviderTest {

    PersonNameSuggestionProvider autoCompleter;

    @Test(expected = NullPointerException.class)
    public void initAutoCompleterWithNullFieldThrowsException() {
        new PersonNameSuggestionProvider((String) null);
    }

    @Before
    public void setUp() throws Exception {
        autoCompleter = new PersonNameSuggestionProvider("field");
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        autoCompleter.indexEntry(null);

        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "testTitle");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeNameReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Testname");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Testname")));
        Assert.assertEquals(Arrays.asList("Testname"), result);
    }

    @Test
    public void completeBeginnigOfNameReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Testname");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Test")));
        Assert.assertEquals(Arrays.asList("Testname"), result);
    }

    @Test
    public void completeLowercaseNameReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Testname");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        Assert.assertEquals(Arrays.asList("Testname"), result);
    }

    @Test
    public void completeNullReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest((null)));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "testKey");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        BibEntry entryOne = new BibEntry();
        entryOne.setField("field", "testNameOne");
        autoCompleter.indexEntry(entryOne);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField("field", "testNameTwo");
        autoCompleter.indexEntry(entryTwo);

        Collection<Author> result = autoCompleter.call(getRequest(("testName")));
        Assert.assertEquals(Arrays.asList("testNameOne", "testNameTwo"), result);
    }

    @Test
    public void completePartOfNameReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("osta")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Vas")));
        Assert.assertEquals(Arrays.asList("Vassilis Kostakos"), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteNameWithJr() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Reagle, Jr., Joseph M.");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Jos")));
        Assert.assertEquals(Arrays.asList("Joseph M. Reagle, Jr."), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsCompleteNameWithVon() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Eric")));
        Assert.assertEquals(Arrays.asList("Eric von Hippel"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithUmlauts() {
        //when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_FULL);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Honig Bär");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Bä")));
        Assert.assertEquals(Arrays.asList("Bär, Honig"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameAndNameWithInitialFirstname() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kosta")));
        Assert.assertEquals(Arrays.asList("Kostakos, V.", "Kostakos, Vassilis"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameIfPref() {
        //when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_FULL);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kosta")));
        Assert.assertEquals(Arrays.asList("Kostakos, Vassilis"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithJrIfPref() {
        //when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Reagle, Jr., Joseph M.");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Rea")));
        Assert.assertEquals(Arrays.asList("Reagle, Jr., J. M."), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithInitialFirstnameIfPref() {
        //when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kosta")));
        Assert.assertEquals(Arrays.asList("Kostakos, V."), result);
    }

    @Test
    public void completeVonReturnsNameWithInitialFirstnameIfPref() {
        //when(preferences.getFirstnameMode()).thenReturn(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("von")));
        Assert.assertEquals(Arrays.asList("von Hippel, E."), result);
    }

    @Test
    public void completeBeginningOfNameReturnsCompleteName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kostakos, Va")));
        Assert.assertEquals(Arrays.asList("Kostakos, Vassilis"), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNothingIfPref() {
        //when(preferences.getOnlyCompleteFirstLast()).thenReturn(true);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kosta")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsNothingIfPref() {
        //when(preferences.getOnlyCompleteLastFirst()).thenReturn(true);

        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Vas")));
        Assert.assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeShortNameReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "nam");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("n")));
        Assert.assertEquals(Arrays.asList("nam"), result);
    }
}
