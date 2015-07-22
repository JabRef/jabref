package net.sf.jabref.search.rules;

import net.sf.jabref.*;

import net.sf.jabref.search.rules.BasicRegexSearchRule;
import net.sf.jabref.search.rules.BasicSearchRule;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test case for BasicSearchRule.
 */
public class BasicSearchRuleTest {

    @Test
    public void testBasicSearchParsing() {
        Globals.prefs = JabRefPreferences.getInstance();

        BibtexEntry be = makeBibtexEntry();
        BasicSearchRule bsCaseSensitive = new BasicSearchRule(true);
        BasicSearchRule bsCaseInsensitive = new BasicSearchRule(false);
        BasicSearchRule bsCaseSensitiveRegexp = new BasicRegexSearchRule(true);
        BasicSearchRule bsCaseInsensitiveRegexp = new BasicRegexSearchRule(false);

        String query = "marine 2001 shields";

        Assert.assertEquals(0, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(1, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(0, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(1, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine larviculture\"";

        Assert.assertEquals(0, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(0, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(0, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(0, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine [A-Za-z]* larviculture\"";

        Assert.assertEquals(0, bsCaseSensitive.applyRule(query, be));
        Assert.assertEquals(0, bsCaseInsensitive.applyRule(query, be));
        Assert.assertEquals(0, bsCaseSensitiveRegexp.applyRule(query, be));
        Assert.assertEquals(1, bsCaseInsensitiveRegexp.applyRule(query, be));

    }

    public BibtexEntry makeBibtexEntry() {
        BibtexEntry e = new BibtexEntry(IdGenerator.next(), BibtexEntryType.INCOLLECTION);
        e.setField("title", "Marine finfish larviculture in Europe");
        e.setField("bibtexkey", "shields01");
        e.setField("year", "2001");
        e.setField("author", "Kevin Shields");
        return e;
    }
}
