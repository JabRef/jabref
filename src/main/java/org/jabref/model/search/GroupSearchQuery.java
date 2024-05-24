package org.jabref.model.search;

import java.util.EnumSet;

import org.jabref.logic.search.SearchQuery;

public class GroupSearchQuery extends SearchQuery {

    public GroupSearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        super(query, searchFlags);
    }

    public String getSearchExpression() {
        return query;
    }
}
