package net.sf.jabref.search;

import net.sf.jabref.Globals;
import net.sf.jabref.SearchRuleSet;
import net.sf.jabref.BibtexEntry;

import java.util.Hashtable;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher for filtering the table according to a SearchRuleSet and a search term. 
 */
public class SearchMatcher implements Matcher {

    private String field = Globals.SEARCH;
        private SearchRuleSet ruleSet;
        private Hashtable searchOptions;

        public SearchMatcher(SearchRuleSet ruleSet, Hashtable searchOptions) {
            this.ruleSet = ruleSet;
            this.searchOptions = searchOptions;
        }
        public boolean matches(Object object) {
            BibtexEntry entry = (BibtexEntry)object;
            int result = ruleSet.applyRule(searchOptions, entry);
            return result > 0;
        }
}
