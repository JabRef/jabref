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
package net.sf.jabref.logic.search;

import net.sf.jabref.logic.search.describer.ContainsAndRegexBasedSearchRuleDescriber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainsAndRegexBasedSearchRuleDescriberTest {

    @Test
    public void testNoAst() throws Exception {
        String query = "a b";
        evaluateNoAst(query, true, true, "This group contains entries in which any field contains the regular expression " +
                "<b>&#97;</b><b>&#98;</b> (case sensitive). Entries cannot be manually assigned to or removed " +
                "from this group.<p><br>Hint: To search specific fields only, " +
                "enter for example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, true, false, "This group contains entries in which any field contains the term " +
                "<b>&#97;</b><b>&#98;</b> (case sensitive). Entries cannot be manually assigned to or removed from " +
                "this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, false, "This group contains entries in which any field contains the term " +
                "<b>&#97;</b><b>&#98;</b> (case insensitive). Entries cannot be manually assigned to or removed " +
                "from this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
        evaluateNoAst(query, false, true, "This group contains entries in which any field contains the regular " +
                "expression <b>&#97;</b><b>&#98;</b> (case insensitive). Entries cannot be manually assigned " +
                "to or removed from this group.<p><br>Hint: To search specific fields only, enter for " +
                "example:<p><tt>author=smith and title=electrical</tt>");
    }

    private void evaluateNoAst(String query, boolean caseSensitive, boolean regex, String expected) {
        assertEquals(expected, new ContainsAndRegexBasedSearchRuleDescriber(caseSensitive, regex, query).getDescription());
    }

}
