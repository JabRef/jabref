package net.sf.jabref.logic.search.rules.describer;

import net.sf.jabref.logic.search.rules.SearchRule;
import net.sf.jabref.logic.search.rules.ContainBasedSearchRule;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.logic.search.rules.RegexBasedSearchRule;

public class SearchDescribers {

    /**
     * Get the search describer for a given search rule and a given search query.
     *
     * @param searchRule the rule that encodes the search logic
     * @param query      the search query
     * @return the search describer to turn the search into something human understandable
     */
    public static SearchDescriber getSearchDescriberFor(SearchRule searchRule, String query) {
        if (searchRule instanceof GrammarBasedSearchRule) {
            GrammarBasedSearchRule grammarBasedSearchRule = (GrammarBasedSearchRule) searchRule;

            return new GrammarBasedSearchRuleDescriber(grammarBasedSearchRule.isCaseSensitiveSearch(), grammarBasedSearchRule.isRegExpSearch(), grammarBasedSearchRule.getTree());
        } else if (searchRule instanceof ContainBasedSearchRule) {
            ContainBasedSearchRule containBasedSearchRule = (ContainBasedSearchRule) searchRule;

            return new ContainsAndRegexBasedSearchRuleDescriber(containBasedSearchRule.isCaseSensitive(), false, query);
        } else if (searchRule instanceof RegexBasedSearchRule) {
            RegexBasedSearchRule regexBasedSearchRule = (RegexBasedSearchRule) searchRule;

            return new ContainsAndRegexBasedSearchRuleDescriber(regexBasedSearchRule.isCaseSensitive(), true, query);
        } else {
            throw new IllegalStateException("Cannot find a describer for searchRule " + searchRule + " and query " + query);
        }
    }

}
