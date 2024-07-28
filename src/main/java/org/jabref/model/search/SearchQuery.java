package org.jabref.model.search;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import static org.jabref.model.search.SearchFlags.FILTERING_SEARCH;
import static org.jabref.model.search.SearchFlags.KEEP_SEARCH_STRING;

public class SearchQuery {

    protected final String query;
    protected Query parsedQuery;
    protected String parseError;
    protected EnumSet<SearchFlags> searchFlags;

    public SearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        this.query = Objects.requireNonNull(query);
        this.searchFlags = searchFlags;

        Map<String, Float> boosts = new HashMap<>();
        SearchFieldConstants.SEARCHABLE_BIB_FIELDS.forEach(field -> boosts.put(field, 4F));

        if (searchFlags.contains(SearchFlags.FULLTEXT)) {
            SearchFieldConstants.PDF_FIELDS.forEach(field -> boosts.put(field, 1F));
        }

        String[] fieldsToSearchArray = new String[boosts.size()];
        boosts.keySet().toArray(fieldsToSearchArray);

        if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
            query = '/' + query + '/';
        }

        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fieldsToSearchArray, SearchFieldConstants.ANALYZER, boosts);
        queryParser.setAllowLeadingWildcard(true);

        if (!query.contains("\"") && !query.contains(":") && !query.contains("*") && !query.contains("~")) {
            query = Arrays.stream(query.split(" ")).map(s -> "*" + s + "*").collect(Collectors.joining(" "));
        }
        try {
            parsedQuery = queryParser.parse(query);
            parseError = null;
        } catch (ParseException e) {
            parsedQuery = null;
            parseError = e.getMessage();
        }
    }

    /**
     * Equals, but only partially compares SearchFlags
     *
     * @return true if the search query is the same except for the filtering/keep search string flags
     */
    public boolean isEqualExceptSearchFlags(SearchQuery other) {
        if (!(other instanceof SearchQuery searchQuery) || !searchQuery.query.equals(this.query)) {
            return false;
        }
        return excludeFilterAndKeepSearchFlags(this.searchFlags).equals(excludeFilterAndKeepSearchFlags(searchQuery.searchFlags));
    }

    private Set<SearchFlags> excludeFilterAndKeepSearchFlags(EnumSet<SearchFlags> searchFlags) {
        EnumSet<SearchFlags> searchRulesWithoutFilterAndSort = searchFlags.clone();
        searchRulesWithoutFilterAndSort.removeAll(EnumSet.of(FILTERING_SEARCH, KEEP_SEARCH_STRING));
        return searchRulesWithoutFilterAndSort;
    }

    @Override
    public String toString() {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SearchQuery searchQuery) {
            return this.query.equals(searchQuery.query) && this.searchFlags.equals(searchQuery.searchFlags);
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
