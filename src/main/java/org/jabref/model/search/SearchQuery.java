package org.jabref.model.search;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

public class SearchQuery {

    protected final String query;
    protected Query parsedQuery;
    protected String parseError;
    protected EnumSet<SearchFlags> searchFlags;

    public SearchQuery(String query, EnumSet<SearchFlags> searchFlags) {
        this.query = Objects.requireNonNull(query);
        this.searchFlags = searchFlags;

        Map<String, Float> boosts = new HashMap<>();
        boosts.put(SearchFieldConstants.DEFAULT_FIELD.toString(), 4F);

        if (searchFlags.contains(SearchFlags.FULLTEXT)) {
            SearchFieldConstants.PDF_FIELDS.forEach(field -> boosts.put(field, 1F));
        }

        String[] fieldsToSearchArray = new String[boosts.size()];
        boosts.keySet().toArray(fieldsToSearchArray);

        if (searchFlags.contains(SearchFlags.REGULAR_EXPRESSION)) {
            query = '/' + query + '/';
        }

        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fieldsToSearchArray, SearchFieldConstants.Standard_ANALYZER, boosts);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchQuery that)) {
            return false;
        }
        return Objects.equals(query, that.query)
                && Objects.equals(searchFlags, that.searchFlags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, searchFlags);
    }

    public boolean isValid() {
        return parseError == null;
    }

    public Query getParsedQuery() {
        return parsedQuery;
    }

    public EnumSet<SearchFlags> getSearchFlags() {
        return searchFlags;
    }
}
