package net.sf.jabref.logic.autocompleter;

import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Test;

public class AutoCompleterFactoryTest {

    @Test(expected = NullPointerException.class)
    public void initFactoryWithNullPreferenceThrowsException() {
        new AutoCompleterFactory(null);
    }

    @Test
    public void getForUnknownFieldReturnsDefaultAutoCompleter() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        AutoCompleter<String> autoCompleter = autoCompleterFactory.forField("unknownField");
        Assert.assertTrue(autoCompleter instanceof DefaultAutoCompleter);
    }

    @Test(expected = NullPointerException.class)
    public void getForNullThrowsException() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        autoCompleterFactory.forField(null);
    }

    @Test
    public void getForAuthorReturnsNameFieldAutoCompleter() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        AutoCompleter<String> autoCompleter = autoCompleterFactory.forField("author");
        Assert.assertTrue(autoCompleter instanceof NameFieldAutoCompleter);
    }

    @Test
    public void getForEditorReturnsNameFieldAutoCompleter() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        AutoCompleter<String> autoCompleter = autoCompleterFactory.forField("editor");
        Assert.assertTrue(autoCompleter instanceof NameFieldAutoCompleter);
    }

    @Test
    public void getForCrossrefReturnsBibtexKeyAutoCompleter() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        AutoCompleter<String> autoCompleter = autoCompleterFactory.forField("crossref");
        Assert.assertTrue(autoCompleter instanceof BibtexKeyAutoCompleter);
    }

    @Test
    public void getForJournalReturnsEntireFieldAutoCompleter() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        AutoCompleter<String> autoCompleter = autoCompleterFactory.forField("journal");
        Assert.assertTrue(autoCompleter instanceof EntireFieldAutoCompleter);
    }

    @Test
    public void getForPublisherReturnsEntireFieldAutoCompleter() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        AutoCompleter<String> autoCompleter = autoCompleterFactory.forField("publisher");
        Assert.assertTrue(autoCompleter instanceof EntireFieldAutoCompleter);
    }

    @Test
    public void getPersonAutoCompleterReturnsNameFieldAutoCompleter() {
        AutoCompletePreferences preferences = mock(AutoCompletePreferences.class);
        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        AutoCompleter<String> autoCompleter = autoCompleterFactory.getPersonAutoCompleter();
        Assert.assertTrue(autoCompleter instanceof NameFieldAutoCompleter);
    }
}
