package org.jabref.model.search;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import static org.jabref.model.search.SearchFlags.FILTERING_SEARCH;
import static org.jabref.model.search.SearchFlags.KEEP_SEARCH_STRING;
import static org.jabref.model.search.SearchFlags.SORT_BY_SCORE;

public class SearchQuery {

    protected final String query;
    protected Query parsedQuery;
    protected String parseError;
    protected EnumSet<SearchFlags> searchFlags;

    public SearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        this.query = Objects.requireNonNull(query);
        this.searchFlags = searchFlags;

        HashMap<String, Float> boosts = new HashMap<>();
        SearchFieldConstants.SEARCHABLE_BIB_FIELDS.forEach(field -> boosts.put(field, 4F));

        if (searchFlags.contains(SearchFlags.FULLTEXT)) {
            Arrays.stream(SearchFieldConstants.PDF_FIELDS).forEach(field -> boosts.put(field, 1F));
        }

        String[] fieldsToSearchArray = new String[boosts.size()];
        boosts.keySet().toArray(fieldsToSearchArray);

        if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
            if (!query.isEmpty() && !(query.startsWith("/") && query.endsWith("/"))) {
                query = '/' + query + '/';
            }
        }

        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fieldsToSearchArray, SearchFieldConstants.ANALYZER, boosts);
        queryParser.setAllowLeadingWildcard(true);

        try {
            parsedQuery = queryParser.parse(query);
            parseError = null;
        } catch (ParseException e) {
            parsedQuery = null;
            parseError = e.getMessage();
        }
    }

    @Override
    public String toString() {
        return query;
    }

    /**
     * Equals, but only partially compares SearchFlags
     *
     * @return true if the search query is the same except for the filtering/sorting flags
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof SearchQuery searchQuery) {
            if (!searchQuery.query.equals(this.query)) {
                return false;
            }
            Set<SearchFlags> thisSearchRulesWithoutFilterAndSort = this.searchFlags.clone();
            Set<SearchFlags> otherSearchRulesWithoutFilterAndSort = searchQuery.searchFlags.clone();
            Set<SearchFlags> filterAndSortFlags = EnumSet.of(SORT_BY_SCORE, FILTERING_SEARCH, KEEP_SEARCH_STRING);
            thisSearchRulesWithoutFilterAndSort.removeAll(filterAndSortFlags);
            otherSearchRulesWithoutFilterAndSort.removeAll(filterAndSortFlags);
            return thisSearchRulesWithoutFilterAndSort.equals(otherSearchRulesWithoutFilterAndSort);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.query.hashCode();
    }

    public boolean isValid() {
        return parseError == null;
    }

    public Query getQuery() {
        return parsedQuery;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }
}
