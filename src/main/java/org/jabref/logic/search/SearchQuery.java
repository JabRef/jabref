package org.jabref.logic.search;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.model.pdf.search.EnglishStemAnalyzer;
import org.jabref.model.pdf.search.SearchFieldConstants;
import org.jabref.model.search.rules.SearchRules;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

public class SearchQuery {

    protected final String query;
    protected Query parsedQuery;
    protected String parseError;
    protected EnumSet<SearchRules.SearchFlags> searchFlags;

    public SearchQuery(String query, EnumSet<SearchRules.SearchFlags> searchFlags) {
        this.query = Objects.requireNonNull(query);
        this.searchFlags = searchFlags;

        HashMap<String, Float> boosts = new HashMap<>();
        SearchFieldConstants.searchableBibFields.forEach(field -> boosts.put(field, Float.valueOf(4)));

        if (searchFlags.contains(SearchRules.SearchFlags.FULLTEXT)) {
            Arrays.stream(SearchFieldConstants.PDF_FIELDS).forEach(field -> boosts.put(field, Float.valueOf(1)));
        }
        String[] fieldsToSearchArray = new String[boosts.size()];
        boosts.keySet().toArray(fieldsToSearchArray);

        if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
            if (query.length() > 0 && !(query.startsWith("/") && query.endsWith("/"))) {
                query = "/" + query + "/";
            }
        }

        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fieldsToSearchArray, new EnglishStemAnalyzer(), boosts);
        queryParser.setAllowLeadingWildcard(true);
        if (!query.contains("\"") && !query.contains(":") && !query.contains("*") && !query.contains("~") & query.length() > 0) {
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

    @Override
    public String toString() {
        return query;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SearchQuery searchQuery) {
        return other.query...
            return ((SearchQuery) other).query.equals(this.query) && ((SearchQuery) other).searchFlags.equals(this.searchFlags);
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

    public EnumSet<SearchRules.SearchFlags> getSearchFlags() {
        return searchFlags;
    }
}
