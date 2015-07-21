package net.sf.jabref.search.rules.sets;


public class SearchRuleSets {

    public enum RuleSetType {
        AND, OR, NONE;
    }

    public static SearchRuleSet build(RuleSetType ruleSet) {
        if (ruleSet == RuleSetType.AND) {
            return new AndSearchRuleSet();
        } else if (ruleSet == RuleSetType.OR) {
            return new OrSearchRuleSet();
        } else {
            return new SearchRuleSet();
        }
    }

}
