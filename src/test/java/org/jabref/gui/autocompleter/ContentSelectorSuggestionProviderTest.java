package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContentSelectorSuggestionProviderTest {

    private ContentSelectorSuggestionProvider autoCompleter;

    @Test
    void completeWithoutAddingAnythingReturnsNothing() {
        SuggestionProvider<String> suggestionProvider = new EmptySuggestionProvider();
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Collections.emptyList());

        Collection<String> expected = Collections.emptyList();
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));

        assertEquals(expected, result);
    }

    @Test
    void completeKeywordReturnsKeyword() {
        SuggestionProvider<String> suggestionProvider = new EmptySuggestionProvider();
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Collections.singletonList("test"));

        Collection<String> expected = Collections.singletonList("test");
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));

        assertEquals(expected, result);
    }

    @Test
    void completeBeginningOfKeywordReturnsKeyword() {
        SuggestionProvider<String> suggestionProvider = new EmptySuggestionProvider();
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Collections.singletonList("test"));

        Collection<String> expected = Collections.singletonList("test");
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("te")));

        assertEquals(expected, result);
    }

    @Test
    void completeKeywordReturnsKeywordFromDatabase() {
        BibDatabase database = new BibDatabase();
        BibEntry bibEntry = new BibEntry();
        bibEntry.addKeyword("test", ',');
        database.insertEntry(bibEntry);

        SuggestionProvider<String> suggestionProvider = new WordSuggestionProvider(StandardField.KEYWORDS, database);
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Collections.emptyList());

        Collection<String> expected = Collections.singletonList("test");
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));

        assertEquals(expected, result);
    }

    @Test
    void completeUppercaseBeginningOfNameReturnsName() {
        SuggestionProvider<String> suggestionProvider = new EmptySuggestionProvider();
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Collections.singletonList("test"));

        Collection<String> expected = Collections.singletonList("test");
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("TE")));

        assertEquals(expected, result);
    }

    @Test
    void completeNullThrowsException() {
        assertThrows(NullPointerException.class, () -> autoCompleter.provideSuggestions(getRequest((null))));
    }

    @Test
    void completeEmptyStringReturnsNothing() {
        SuggestionProvider<String> suggestionProvider = new EmptySuggestionProvider();
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Collections.singletonList("test"));

        Collection<String> expected = Collections.emptyList();
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("")));

        assertEquals(expected, result);
    }

    @Test
    void completeReturnsMultipleResults() {
        BibDatabase database = new BibDatabase();
        BibEntry bibEntry = new BibEntry();
        bibEntry.addKeyword("testa", ',');
        database.insertEntry(bibEntry);

        SuggestionProvider<String> suggestionProvider = new WordSuggestionProvider(StandardField.KEYWORDS, database);
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Collections.singletonList("testb"));

        Collection<String> expected = Arrays.asList("testa", "testb");
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));

        assertEquals(expected, result);
    }

    @Test
    void completeReturnsKeywordsInAlphabeticalOrder() {
        BibDatabase database = new BibDatabase();
        BibEntry bibEntry = new BibEntry();
        bibEntry.addKeyword("testd", ',');
        bibEntry.addKeyword("testc", ',');
        database.insertEntry(bibEntry);

        SuggestionProvider<String> suggestionProvider = new WordSuggestionProvider(StandardField.KEYWORDS, database);
        autoCompleter = new ContentSelectorSuggestionProvider(suggestionProvider, Arrays.asList("testb", "testa"));

        Collection<String> expected = Arrays.asList("testa", "testb", "testc", "testd");
        Collection<String> result = autoCompleter.provideSuggestions(getRequest(("test")));

        assertEquals(expected, result);
    }
}
