package org.jabref.model.search.rules;

import java.util.EnumSet;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Disabled;
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

    @Disabled
    @Test
    void searchForAnyFieldWorks() {
        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));

        String query = "anyfield:fruit";
        assertTrue(searchRule.validateSearchStrings(query));
        assertTrue(searchRule.applyRule(query, new BibEntry()
                .withField(StandardField.KEYWORDS, "fruit")));
    }

    @Disabled
    @Test
    void searchForAnyKeywordWorks() {
        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));

        String query = "anykeyword:apple";
        assertTrue(searchRule.validateSearchStrings(query));
        assertTrue(searchRule.applyRule(query, new BibEntry()
                .withField(StandardField.KEYWORDS, "apple")));
        assertFalse(searchRule.applyRule(query, new BibEntry()
                .withField(StandardField.KEYWORDS, "pineapple")));
    }

    @Test
    void searchForCitationKeyWorks() {
        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.noneOf(SearchRules.SearchFlags.class));
        String query = "citationkey==miller2005";
        assertTrue(searchRule.validateSearchStrings(query));
        assertTrue(searchRule.applyRule(query, new BibEntry()
                .withCitationKey("miller2005")));
    }

    @Test
    void searchForThesisEntryTypeWorks() {
        GrammarBasedSearchRule searchRule = new GrammarBasedSearchRule(EnumSet.noneOf(SearchRules.SearchFlags.class));
        String query = "entrytype=thesis";
        assertTrue(searchRule.validateSearchStrings(query));
        assertTrue(searchRule.applyRule(query, new BibEntry(StandardEntryType.PhdThesis)));
    }

    public BibEntry makeBibtexEntry() {
        return new BibEntry(StandardEntryType.InCollection)
                .withCitationKey("shields01")
                .withField(StandardField.TITLE, "Marine finfish larviculture in Europe")
                .withField(StandardField.YEAR, "2001")
                .withField(StandardField.AUTHOR, "Kevin Shields");
    }
}
