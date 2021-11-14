package org.jabref.model.search.rules;

import java.util.EnumSet;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for GrammarBasedSearchRuleTest.
 */
public class GrammarBasedSearchRuleTest {

    @Test
    void applyRuleMatchesSingleTermWithRegex() {
        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));

        String query = "M[a-z]+e";
        assertTrue(searchRule.validateSearchStrings(query));
        assertTrue(searchRule.applyRule(query, makeBibtexEntry()));
    }

    @Test
    void applyRuleDoesNotMatchSingleTermWithRegex() {
        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));

        String query = "M[0-9]+e";
        assertTrue(searchRule.validateSearchStrings(query));
        assertFalse(searchRule.applyRule(query, makeBibtexEntry()));
    }

    @Test
    void searchRuleOfDocumentationMatches() {
        // FIXME: Even though we do not provide a regex, the following instantiation does not match anything:
        //        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.noneOf(SearchRules.SearchFlags.class));
        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));

        String query = "(author = miller or title|keywords = \"image processing\") and not author = brown";
        assertTrue(searchRule.validateSearchStrings(query));
        assertTrue(searchRule.applyRule(query, new BibEntry()
                .withCitationKey("key")
                .withField(StandardField.KEYWORDS, "image processing")));
        assertFalse(searchRule.applyRule(query, new BibEntry()
                .withCitationKey("key")
                .withField(StandardField.AUTHOR, "Sam Brown")
                .withField(StandardField.KEYWORDS, "image processing")));
    }

    public BibEntry makeBibtexEntry() {
        return new BibEntry(StandardEntryType.InCollection)
                .withCitationKey("shields01")
                .withField(StandardField.TITLE, "Marine finfish larviculture in Europe")
                .withField(StandardField.YEAR, "2001")
                .withField(StandardField.AUTHOR, "Kevin Shields");
    }
}
