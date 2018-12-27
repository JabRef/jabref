package org.jabref.model.search.matchers;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.rules.MockSearchMatcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
