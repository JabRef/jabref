package org.jabref.logic.autocompleter;

import org.jabref.logic.journals.JournalAbbreviationLoader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class AutoCompleterFactoryTest {

    private AutoCompleterFactory autoCompleterFactory;
    private JournalAbbreviationLoader abbreviationLoader;

    @Before
    public void setUp() throws Exception {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        abbreviationLoader = mock(JournalAbbreviationLoader.class);
        autoCompleterFactory = new AutoCompleterFactory(preferences, abbreviationLoader);
    }

    @Test(expected = NullPointerException.class)
    public void initFactoryWithNullPreferenceThrowsException() {
        new AutoCompleterFactory(null, abbreviationLoader);
    }

    @Test
    public void getForUnknownFieldReturnsDefaultAutoCompleter() {
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor("unknownField");
        Assert.assertTrue(autoCompleter instanceof DefaultAutoCompleter);
    }

    @Test(expected = NullPointerException.class)
    public void getForNullThrowsException() {
        autoCompleterFactory.getFor(null);
    }

    @Test
    public void getForAuthorReturnsNameFieldAutoCompleter() {
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor("author");
        Assert.assertTrue(autoCompleter instanceof NameFieldAutoCompleter);
    }

    @Test
    public void getForEditorReturnsNameFieldAutoCompleter() {
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor("editor");
        Assert.assertTrue(autoCompleter instanceof NameFieldAutoCompleter);
    }

    @Test
    public void getForCrossrefReturnsBibtexKeyAutoCompleter() {
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor("crossref");
        Assert.assertTrue(autoCompleter instanceof BibtexKeyAutoCompleter);
    }

    @Test
    public void getForJournalReturnsEntireFieldAutoCompleter() {
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor("journal");
        Assert.assertTrue(autoCompleter instanceof EntireFieldAutoCompleter);
    }

    @Test
    public void getForPublisherReturnsEntireFieldAutoCompleter() {
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor("publisher");
        Assert.assertTrue(autoCompleter instanceof EntireFieldAutoCompleter);
    }

    @Test
    public void getPersonAutoCompleterReturnsNameFieldAutoCompleter() {
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getPersonAutoCompleter();
        Assert.assertTrue(autoCompleter instanceof NameFieldAutoCompleter);
    }
}
