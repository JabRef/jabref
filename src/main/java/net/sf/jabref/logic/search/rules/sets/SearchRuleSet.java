/*
 * Copyright (C) 2003 Nathan Dunn.
 * Copyright (C) 2015 Simon Harrer
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
package net.sf.jabref.logic.search.rules.sets;

import net.sf.jabref.logic.search.SearchRule;

import java.util.Objects;
import java.util.Vector;

public abstract class SearchRuleSet implements SearchRule {

    protected final Vector<SearchRule> ruleSet = new Vector<>();

    public void addRule(SearchRule newRule) {
        ruleSet.add(Objects.requireNonNull(newRule));
    }

    @Override
    public boolean validateSearchStrings(String query) {
        for (SearchRule searchRule : ruleSet) {
            if (!searchRule.validateSearchStrings(query)) {
                return false;
            }
        }
        return true;
    }
}
