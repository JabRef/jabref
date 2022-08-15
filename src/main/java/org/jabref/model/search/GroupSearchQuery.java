package org.jabref.model.search;

import java.util.EnumSet;

import org.jabref.logic.search.SearchQuery;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

public class GroupSearchQuery extends SearchQuery {

    public GroupSearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        super(query, searchFlags);
    }

    @Override
    public boolean isMatch(BibEntry entry) {
        return this.getRule().applyRule(query, entry);
    }

    public String getSearchExpression() {
        return query;
    }
}
