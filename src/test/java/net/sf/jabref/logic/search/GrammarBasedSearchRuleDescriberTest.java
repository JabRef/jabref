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

import net.sf.jabref.logic.search.describer.GrammarBasedSearchRuleDescriber;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;
import org.junit.Test;

import static org.junit.Assert.*;

public class GrammarBasedSearchRuleDescriberTest {

    @Test
    public void testSimpleQuery() throws Exception {
        String query = "a=b";
        evaluate(query, true, true, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the Regular Expression <b>&#98;</b>. " +
                "The search is case sensitive.");
        evaluate(query, true, false, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the term <b>&#98;</b>. " +
                "The search is case sensitive.");
        evaluate(query, false, false, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the term <b>&#98;</b>. " +
                "The search is case insensitive.");
        evaluate(query, false, true, "This group contains entries in which the field <b>&#97;</b> " +
                "contains the Regular Expression <b>&#98;</b>. " +
                "The search is case insensitive.");
    }

    @Test
    public void testComplexQuery() throws Exception {
        String query = "not a=b and c=e or e=\"x\"";
        evaluate(query, true, true, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the Regular Expression <b>&#98;</b> and the field <b>&#99;</b> contains the "
                + "Regular Expression <b>&#101;</b>) or the field <b>&#101;</b> contains the Regular Expression "
                + "<b>&#120;</b>). The search is case sensitive.");
        evaluate(query, true, false, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the term <b>&#98;</b> and the field <b>&#99;</b> contains the term <b>&#101;</b>) "
                + "or the field <b>&#101;</b> contains the term <b>&#120;</b>). The search is case sensitive.");
        evaluate(query, false, false, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the term <b>&#98;</b> and the field <b>&#99;</b> contains the term <b>&#101;</b>) "
                + "or the field <b>&#101;</b> contains the term <b>&#120;</b>). The search is case insensitive.");
        evaluate(query, false, true, "This group contains entries in which not ((the field <b>&#97;</b> "
                + "contains the Regular Expression <b>&#98;</b> and the field <b>&#99;</b> contains "
                + "the Regular Expression <b>&#101;</b>) or the field <b>&#101;</b> contains the Regular "
                + "Expression <b>&#120;</b>). The search is case insensitive.");
    }



    private void evaluate(String query, boolean caseSensitive, boolean regex, String expected) {
        GrammarBasedSearchRule grammarBasedSearchRule = new GrammarBasedSearchRule(caseSensitive, regex);
        assertTrue(grammarBasedSearchRule.validateSearchStrings(query));
        GrammarBasedSearchRuleDescriber describer = new GrammarBasedSearchRuleDescriber(caseSensitive, regex, grammarBasedSearchRule.getTree());
        assertEquals(expected, describer.getDescription());
    }
}