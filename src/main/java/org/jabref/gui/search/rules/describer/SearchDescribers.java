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
        if (searchQuery.getRule() instanceof GrammarBasedSearchRule grammarBasedSearchRule) {
            return new GrammarBasedSearchRuleDescriber(grammarBasedSearchRule.getSearchFlags(), grammarBasedSearchRule.getTree());
        } else if (searchQuery.getRule() instanceof ContainBasedSearchRule containBasedSearchRule) {
            return new ContainsAndRegexBasedSearchRuleDescriber(containBasedSearchRule.getSearchFlags(), searchQuery.getQuery());
        } else if (searchQuery.getRule() instanceof RegexBasedSearchRule regexBasedSearchRule) {
            return new ContainsAndRegexBasedSearchRuleDescriber(regexBasedSearchRule.getSearchFlags(), searchQuery.getQuery());
        } else {
            throw new IllegalStateException("Cannot find a describer for searchRule " + searchQuery.getRule() + " and query " + searchQuery.getQuery());
        }
    }
}
