package tests.net.sf.jabref.search;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Util;
import net.sf.jabref.search.BasicSearch;

/**
 * Test case for BasicSearch.
 */
public class BasicSearchTest extends TestCase {

    public void testBasicSearchParsing() {

        BibtexEntry be = makeBibtexEntry();
        BasicSearch bsCaseSensitive = new BasicSearch(true, false);
        BasicSearch bsCaseInsensitive = new BasicSearch(false, false);
        BasicSearch bsCaseSensitiveRegexp = new BasicSearch(true, true);
        BasicSearch bsCaseInsensitiveRegexp = new BasicSearch(false, true);

        String query = "marine 2001 shields";
        
        assertEquals(0, bsCaseSensitive.applyRule(query, be));
        assertEquals(1, bsCaseInsensitive.applyRule(query, be));
        assertEquals(0, bsCaseSensitiveRegexp.applyRule(query, be));
        assertEquals(1, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine larviculture\"";

        assertEquals(0, bsCaseSensitive.applyRule(query, be));
        assertEquals(0, bsCaseInsensitive.applyRule(query, be));
        assertEquals(0, bsCaseSensitiveRegexp.applyRule(query, be));
        assertEquals(0, bsCaseInsensitiveRegexp.applyRule(query, be));

        query = "\"marine [A-Za-z]* larviculture\"";

        assertEquals(0, bsCaseSensitive.applyRule(query, be));
        assertEquals(0, bsCaseInsensitive.applyRule(query, be));
        assertEquals(0, bsCaseSensitiveRegexp.applyRule(query, be));
        assertEquals(1, bsCaseInsensitiveRegexp.applyRule(query, be));

        
    }

    public BibtexEntry makeBibtexEntry() {
		BibtexEntry e = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.INCOLLECTION);
		e.setField("title", "Marine finfish larviculture in Europe");
		e.setField("bibtexkey", "shields01");
		e.setField("year", "2001");
		e
			.setField(
				"author",
				"Kevin Shields");
		return e;
	}
}

