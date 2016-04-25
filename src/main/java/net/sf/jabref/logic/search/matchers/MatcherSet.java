/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.logic.search.matchers;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

import net.sf.jabref.logic.search.SearchMatcher;

public abstract class MatcherSet implements SearchMatcher {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MatcherSet that = (MatcherSet) o;

        return matchers.equals(that.matchers);

    }

    @Override
    public int hashCode() {
        return matchers.hashCode();
    }

    protected final List<SearchMatcher> matchers = new Vector<>();

    public void addRule(SearchMatcher newRule) {
        matchers.add(Objects.requireNonNull(newRule));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MatcherSet{");
        sb.append("matchers=").append(matchers);
        sb.append('}');
        return sb.toString();
    }

}
