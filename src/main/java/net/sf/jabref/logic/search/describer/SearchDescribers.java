/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.search.describer;

import net.sf.jabref.logic.search.SearchRule;
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

            return new GrammarBasedSearchRuleDescriber(grammarBasedSearchRule.isCaseSensitiveSearch(),
                    grammarBasedSearchRule.isRegExpSearch(), grammarBasedSearchRule.getTree());
        } else if (searchRule instanceof ContainBasedSearchRule) {
            ContainBasedSearchRule containBasedSearchRule = (ContainBasedSearchRule) searchRule;

            return new ContainsAndRegexBasedSearchRuleDescriber(containBasedSearchRule.isCaseSensitive(), false, query);
        } else if (searchRule instanceof RegexBasedSearchRule) {
            RegexBasedSearchRule regexBasedSearchRule = (RegexBasedSearchRule) searchRule;

            return new ContainsAndRegexBasedSearchRuleDescriber(regexBasedSearchRule.isCaseSensitive(), true, query);
        } else {
            throw new IllegalStateException("Cannot find a describer for searchRule " +
                    searchRule + " and query " + query);
        }
    }

}
