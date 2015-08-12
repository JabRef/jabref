/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.search.rules;

import net.sf.jabref.*;

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
