package org.jabref.model.search.matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.model.search.SearchMatcher;

public abstract class MatcherSet implements SearchMatcher {

    protected final List<SearchMatcher> matchers = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        MatcherSet that = (MatcherSet) o;

        return Objects.equals(matchers, that.matchers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchers);
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
