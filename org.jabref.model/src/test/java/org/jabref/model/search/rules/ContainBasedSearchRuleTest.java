package org.jabref.model.search.rules;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;

import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals(false, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(true, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(false, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine larviculture\"";

        Assert.assertEquals(false, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(false, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "marine [A-Za-z]* larviculture";

        Assert.assertEquals(false, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(true, bsCaseInsensitiveRegexp.applyRule(query, be));

    }

    public BibEntry makeBibtexEntry() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INCOLLECTION.getName());
        e.setField("title", "Marine finfish larviculture in Europe");
        e.setField("bibtexkey", "shields01");
        e.setField("year", "2001");
        e.setField("author", "Kevin Shields");
        return e;
    }
}
