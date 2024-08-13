package org.jabref.model.search;

import java.util.EnumSet;

public class GroupSearchQuery extends SearchQuery {

    public GroupSearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        super(query, searchFlags);
    }
}
