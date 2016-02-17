package net.sf.jabref.logic.search.matchers;

import net.sf.jabref.logic.search.rules.MockSearchRule;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MatcherSetsTest {

    @Test
    public void testBuildAnd() throws Exception {
        MatcherSet matcherSet = MatcherSets.build(MatcherSets.MatcherType.AND);
        assertTrue(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchRule(true));
        assertTrue(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchRule(false));
        assertFalse(matcherSet.isMatch(new BibEntry()));
    }

    @Test
    public void testBuildOr() throws Exception {
        MatcherSet matcherSet = MatcherSets.build(MatcherSets.MatcherType.OR);
        assertFalse(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchRule(true));
        assertTrue(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchRule(false));
        assertTrue(matcherSet.isMatch(new BibEntry()));
    }

    @Test
    public void testBuildNotWithTrue() {
        NotMatcher matcher = new NotMatcher(new MockSearchRule(true));
        assertFalse(matcher.isMatch(new BibEntry()));
    }

    @Test
    public void testBuildNotWithFalse() {
        NotMatcher matcher = new NotMatcher(new MockSearchRule(false));
        assertTrue(matcher.isMatch(new BibEntry()));
    }

}
