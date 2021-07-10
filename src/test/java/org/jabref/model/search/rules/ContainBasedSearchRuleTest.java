package org.jabref.model.search.rules;

import java.util.EnumSet;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for ContainBasedSearchRule.
 */
public class ContainBasedSearchRuleTest {

    @Test
    public void testBasicSearchParsing() {
        BibEntry be = makeBibtexEntry();
        ContainBasedSearchRule bsCaseSensitive = new ContainBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        ContainBasedSearchRule bsCaseInsensitive = new ContainBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));
        RegexBasedSearchRule bsCaseSensitiveRegexp = new RegexBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION));
        RegexBasedSearchRule bsCaseInsensitiveRegexp = new RegexBasedSearchRule(EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));

        String query = "marine 2001 shields";

        assertFalse(bsCaseSensitive.applyRule(query, be));
        assertTrue(bsCaseInsensitive.applyRule(query, be));
        assertFalse(bsCaseSensitiveRegexp.applyRule(query, be));
        assertFalse(bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine larviculture\"";

        assertFalse(bsCaseSensitive.applyRule(query, be));
        assertFalse(bsCaseInsensitive.applyRule(query, be));
        assertFalse(bsCaseSensitiveRegexp.applyRule(query, be));
        assertFalse(bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "marine [A-Za-z]* larviculture";

        assertFalse(bsCaseSensitive.applyRule(query, be));
        assertFalse(bsCaseInsensitive.applyRule(query, be));
        assertFalse(bsCaseSensitiveRegexp.applyRule(query, be));
        assertTrue(bsCaseInsensitiveRegexp.applyRule(query, be));
    }

    public BibEntry makeBibtexEntry() {
        return new BibEntry(StandardEntryType.InCollection)
                .withCitationKey("shields01")
                .withField(StandardField.TITLE, "Marine finfish larviculture in Europe")
                .withField(StandardField.YEAR, "2001")
                .withField(StandardField.AUTHOR, "Kevin Shields");
    }
}
