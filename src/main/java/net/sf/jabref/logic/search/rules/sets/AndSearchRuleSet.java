/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.search.rules.sets;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.search.SearchRule;

/**
 * Subclass of SearchRuleSet that ANDs or ORs between its rules, returning 0 or
 * 1.
 */
public class AndSearchRuleSet extends SearchRuleSet {

    @Override
    public boolean applyRule(String searchString, BibEntry bibEntry) {
        int score = 0;

        // We let each rule add a maximum of 1 to the score.
        for (SearchRule rule : ruleSet) {
            if(rule.applyRule(searchString, bibEntry)) {
                score++;
            }
        }

        // Then an AND rule demands that score == number of rules
        return score == ruleSet.size();
    }
}
