package org.jabref.model.search.rules;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case for ContainBasedSearchRule.
 */
public class ContainBasedSearchRuleTest {

    @Test
    public void testBasicSearchParsing() {
        BibEntry be = makeBibtexEntry();
        ContainBasedSearchRule bsCaseSensitive = new ContainBasedSearchRule(true);
        ContainBasedSearchRule bsCaseInsensitive = new ContainBasedSearchRule(false);
        RegexBasedSearchRule bsCaseSensitiveRegexp = new RegexBasedSearchRule(true);
        RegexBasedSearchRule bsCaseInsensitiveRegexp = new RegexBasedSearchRule(false);

        String query = "marine 2001 shields";

        assertEquals(false, bsCaseSensitive.applyRule(query, be));
        assertEquals(true, bsCaseInsensitive.applyRule(query, be));
        assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        assertEquals(false, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine larviculture\"";

        assertEquals(false, bsCaseSensitive.applyRule(query, be));
        assertEquals(false, bsCaseInsensitive.applyRule(query, be));
        assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        assertEquals(false, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "marine [A-Za-z]* larviculture";

        assertEquals(false, bsCaseSensitive.applyRule(query, be));
        assertEquals(false, bsCaseInsensitive.applyRule(query, be));
        assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        assertEquals(true, bsCaseInsensitiveRegexp.applyRule(query, be));
    }

    public BibEntry makeBibtexEntry() {
        BibEntry e = new BibEntry(StandardEntryType.InCollection);
        e.setField(StandardField.TITLE, "Marine finfish larviculture in Europe");
        e.setCitationKey("shields01");
        e.setField(StandardField.YEAR, "2001");
        e.setField(StandardField.AUTHOR, "Kevin Shields");
        return e;
    }
}
