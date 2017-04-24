package org.jabref.model.search.matchers;

public class MatcherSets {

    public enum MatcherType {
        AND,
        OR
    }

    public static MatcherSet build(MatcherType ruleSet) {
        if (ruleSet == MatcherType.AND) {
            return new AndMatcher();
        } else {
            return new OrMatcher();
        }
    }

}
