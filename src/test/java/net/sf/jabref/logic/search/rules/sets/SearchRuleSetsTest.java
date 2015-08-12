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
package net.sf.jabref.logic.search.rules.sets;

import net.sf.jabref.logic.search.rules.MockSearchRule;
import org.junit.Test;

import static org.junit.Assert.*;


public class SearchRuleSetsTest {

    public static final String DUMMY_QUERY = "dummy";

    @Test
    public void testBuildAnd() throws Exception {
        SearchRuleSet searchRuleSet = SearchRuleSets.build(SearchRuleSets.RuleSetType.AND);
        assertEquals(true, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(true, true));
        assertEquals(true, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(false, false));
        assertEquals(false, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(false, searchRuleSet.validateSearchStrings(DUMMY_QUERY));
    }

    @Test
    public void testBuildOr() throws Exception {
        SearchRuleSet searchRuleSet = SearchRuleSets.build(SearchRuleSets.RuleSetType.OR);
        assertEquals(false, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(true, true));
        assertEquals(true, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(false, false));
        assertEquals(true, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(false, searchRuleSet.validateSearchStrings(DUMMY_QUERY));
    }

}