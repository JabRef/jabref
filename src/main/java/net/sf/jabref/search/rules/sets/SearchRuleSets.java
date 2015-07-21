package net.sf.jabref.search.rules.sets;


public class SearchRuleSets {

    public enum RuleSetType {
        AND,
        OR;
    }

    public static SearchRuleSet build(RuleSetType ruleSet) {
        if (ruleSet == RuleSetType.AND) {
            return new AndSearchRuleSet();
        } else {
            return new OrSearchRuleSet();
        }
    }

}
