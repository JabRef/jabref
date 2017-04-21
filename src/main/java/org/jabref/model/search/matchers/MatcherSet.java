package org.jabref.model.search.matchers;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

import org.jabref.model.search.SearchMatcher;

public abstract class MatcherSet implements SearchMatcher {

    protected final List<SearchMatcher> matchers = new Vector<>();

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
