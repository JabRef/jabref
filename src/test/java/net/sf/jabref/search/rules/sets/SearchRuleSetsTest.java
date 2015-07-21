package net.sf.jabref.search.rules.sets;

import net.sf.jabref.search.rules.MockSearchRule;
import org.junit.Test;

import static org.junit.Assert.*;


public class SearchRuleSetsTest {

    public static final String DUMMY_QUERY = "dummy";

    @Test
    public void testBuildAnd() throws Exception {
        SearchRuleSet searchRuleSet = SearchRuleSets.build(SearchRuleSets.RuleSetType.AND);
        assertEquals(1, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(1, true));
        assertEquals(1, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(0, false));
        assertEquals(0, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(false, searchRuleSet.validateSearchStrings(DUMMY_QUERY));
    }

    @Test
    public void testBuildOr() throws Exception {
        SearchRuleSet searchRuleSet = SearchRuleSets.build(SearchRuleSets.RuleSetType.OR);
        assertEquals(0, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(1, true));
        assertEquals(1, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(true, searchRuleSet.validateSearchStrings(DUMMY_QUERY));

        searchRuleSet.addRule(new MockSearchRule(0, false));
        assertEquals(1, searchRuleSet.applyRule(DUMMY_QUERY, null));
        assertEquals(false, searchRuleSet.validateSearchStrings(DUMMY_QUERY));
    }

}