package net.sf.jabref.logic.search.matchers;

import net.sf.jabref.logic.search.rules.MockSearchMatcher;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MatcherSetsTest {

    @Test
    public void testBuildAnd() {
        MatcherSet matcherSet = MatcherSets.build(MatcherSets.MatcherType.AND);
        assertTrue(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchMatcher(true));
        assertTrue(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchMatcher(false));
        assertFalse(matcherSet.isMatch(new BibEntry()));
    }

    @Test
    public void testBuildOr() {
        MatcherSet matcherSet = MatcherSets.build(MatcherSets.MatcherType.OR);
        assertFalse(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchMatcher(true));
        assertTrue(matcherSet.isMatch(new BibEntry()));

        matcherSet.addRule(new MockSearchMatcher(false));
        assertTrue(matcherSet.isMatch(new BibEntry()));
    }

    @Test
    public void testBuildNotWithTrue() {
        NotMatcher matcher = new NotMatcher(new MockSearchMatcher(true));
        assertFalse(matcher.isMatch(new BibEntry()));
    }

    @Test
    public void testBuildNotWithFalse() {
        NotMatcher matcher = new NotMatcher(new MockSearchMatcher(false));
        assertTrue(matcher.isMatch(new BibEntry()));
    }

}
