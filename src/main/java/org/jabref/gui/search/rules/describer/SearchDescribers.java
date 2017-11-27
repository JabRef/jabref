package org.jabref.gui.search.rules.describer;

import org.jabref.logic.search.SearchQuery;
import org.jabref.model.search.rules.ContainBasedSearchRule;
import org.jabref.model.search.rules.GrammarBasedSearchRule;
import org.jabref.model.search.rules.RegexBasedSearchRule;

public class SearchDescribers {

    private SearchDescribers() {
    }

    /**
     * Get the search describer for a given search query.
     *
     * @param searchQuery the search query
     * @return the search describer to turn the search into something human understandable
     */
    public static SearchDescriber getSearchDescriberFor(SearchQuery searchQuery) {
        if (searchQuery.getRule() instanceof GrammarBasedSearchRule) {
            GrammarBasedSearchRule grammarBasedSearchRule = (GrammarBasedSearchRule) searchQuery.getRule();

            return new GrammarBasedSearchRuleDescriber(grammarBasedSearchRule.isCaseSensitiveSearch(), grammarBasedSearchRule.isRegExpSearch(), grammarBasedSearchRule.getTree());
        } else if (searchQuery.getRule() instanceof ContainBasedSearchRule) {
            ContainBasedSearchRule containBasedSearchRule = (ContainBasedSearchRule) searchQuery.getRule();

            return new ContainsAndRegexBasedSearchRuleDescriber(containBasedSearchRule.isCaseSensitive(), false, searchQuery.getQuery());
        } else if (searchQuery.getRule() instanceof RegexBasedSearchRule) {
            RegexBasedSearchRule regexBasedSearchRule = (RegexBasedSearchRule) searchQuery.getRule();

            return new ContainsAndRegexBasedSearchRuleDescriber(regexBasedSearchRule.isCaseSensitive(), true, searchQuery.getQuery());
        } else {
            throw new IllegalStateException("Cannot find a describer for searchRule " + searchQuery.getRule() + " and query " + searchQuery.getQuery());
        }
    }

}
