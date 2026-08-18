package org.jabref.gui.search;

import java.util.EnumSet;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HighlighterTest {

    @BeforeEach
    void setUp() {
        SearchPreferences searchPreferences = mock(SearchPreferences.class);
        when(searchPreferences.shouldUsePostgresSearch()).thenReturn(false);

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getSearchPreferences()).thenReturn(searchPreferences);

        Injector.setModelOrService(GuiPreferences.class, preferences);
    }

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }

    /// Regression test for <https://github.com/JabRef/jabref/issues/16539>.
    /// Search terms made only of regex metacharacters used to throw an uncaught
    /// `PatternSyntaxException` while highlighting the preview.
    @ParameterizedTest
    @ValueSource(strings = {"*", "fieldname=*", "anything{", "anything["})
    void highlightHtmlDoesNotThrowForLiteralTermsWithRegexMetacharacters(String searchExpression) {
        SearchQuery searchQuery = new SearchQuery(searchExpression, EnumSet.noneOf(SearchFlags.class));

        assertDoesNotThrow(() -> Highlighter.highlightHtml("<p>Sample text with * character</p>", searchQuery));
    }

    /// Even a deliberately malformed regex (entered via the `=~` operator) must not crash the
    /// preview - the highlighter should just skip highlighting instead of propagating the exception.
    @ParameterizedTest
    @ValueSource(strings = {"anything=~^\\", "anything=~["})
    void highlightHtmlDoesNotThrowForBrokenExplicitRegex(String searchExpression) {
        SearchQuery searchQuery = new SearchQuery(searchExpression, EnumSet.noneOf(SearchFlags.class));

        assertDoesNotThrow(() -> Highlighter.highlightHtml("<p>Sample text</p>", searchQuery));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "{", "["})
    void findMatchPositionsDoesNotThrowForBrokenRegexPattern(String pattern) {
        assertDoesNotThrow(() -> Highlighter.findMatchPositions("Sample text", pattern));
    }

    /// The search bar's "Illegal search expression" indicator relies on this to flag a deliberately
    /// malformed explicit regex search - previously the query was silently accepted and only the
    /// highlighter quietly gave up, leaving the user with no feedback at all.
    @ParameterizedTest
    @ValueSource(strings = {"anything=~^\\", "anything=~["})
    void isValidRegexPatternIsFalseForBrokenExplicitRegex(String searchExpression) {
        SearchQuery searchQuery = new SearchQuery(searchExpression, EnumSet.noneOf(SearchFlags.class));

        assertFalse(Highlighter.isValidRegexPattern(searchQuery));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "fieldname=*", "anything{", "anything[", "anything=~valid.*regex"})
    void isValidRegexPatternIsTrueForLiteralAndValidRegexTerms(String searchExpression) {
        SearchQuery searchQuery = new SearchQuery(searchExpression, EnumSet.noneOf(SearchFlags.class));

        assertTrue(Highlighter.isValidRegexPattern(searchQuery));
    }
}
