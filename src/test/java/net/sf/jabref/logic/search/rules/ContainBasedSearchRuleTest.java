package net.sf.jabref.logic.search.rules;

import net.sf.jabref.*;

import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for ContainBasedSearchRule.
 */
public class ContainBasedSearchRuleTest {

    @Test
    public void testBasicSearchParsing() {
        Globals.prefs = JabRefPreferences.getInstance();

        BibtexEntry be = makeBibtexEntry();
        ContainBasedSearchRule bsCaseSensitive = new ContainBasedSearchRule(true);
        ContainBasedSearchRule bsCaseInsensitive = new ContainBasedSearchRule(false);
        RegexBasedSearchRule bsCaseSensitiveRegexp = new RegexBasedSearchRule(true);
        RegexBasedSearchRule bsCaseInsensitiveRegexp = new RegexBasedSearchRule(false);

        String query = "marine 2001 shields";

        Assert.assertEquals(false, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(true, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(true, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine larviculture\"";

        Assert.assertEquals(false, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(false, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine [A-Za-z]* larviculture\"";

        Assert.assertEquals(false, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(false, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(true, bsCaseInsensitiveRegexp.applyRule(query, be));

    }

    public BibtexEntry makeBibtexEntry() {
        BibtexEntry e = new BibtexEntry(IdGenerator.next(), BibtexEntryTypes.INCOLLECTION);
        e.setField("title", "Marine finfish larviculture in Europe");
        e.setField("bibtexkey", "shields01");
        e.setField("year", "2001");
        e.setField("author", "Kevin Shields");
        return e;
    }
}
