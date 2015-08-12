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
package net.sf.jabref.logic.search.rules;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.logic.search.SearchRule;

import java.util.Objects;

/**
 * Inverts the search result.
 * <p>
 * Example:
 * false --> true
 * true --> false
 */
public class InvertSearchRule implements SearchRule {

    private final SearchRule otherRule;

    public InvertSearchRule(SearchRule otherRule) {
        this.otherRule = Objects.requireNonNull(otherRule);
    }

    @Override
    public boolean applyRule(String query, BibtexEntry bibtexEntry) {
        return !otherRule.applyRule(query, bibtexEntry);
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return this.otherRule.validateSearchStrings(query);
    }
}
