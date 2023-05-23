package org.jabref.logic.importer.fetcher.transformers;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class CiteSeerQueryTransformer extends AbstractQueryTransformer {

    private JSONObject payload = new JSONObject();

    private JSONArray publisher;
    private String queryString;
    private String sortBy; // supported options include "relevance" or "year"

    @Override
    protected String getLogicalAndOperator() {
        return " ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return " ";
    }

    @Override
    protected String handleAuthor(String author) {
        if (!this.getJSONPayload().has("author")) {
            this.getJSONPayload().put("author", new JSONArray());
        }
        return this.getJSONPayload().getJSONArray("author").put(author).toString();
    }

    @Override
    protected String handleTitle(String title) {
        return StringUtil.quoteStringIfSpaceIsContained(title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return StringUtil.quoteStringIfSpaceIsContained(journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return StringUtil.quoteStringIfSpaceIsContained(year);
    }

    @Override
    protected String handleYearRange(String yearRange) {
         parseYearRange(yearRange);
         if (endYear == Integer.MAX_VALUE) {
             return yearRange; // invalid year range
         }
         payload.put("yearStart", startYear);
         payload.put("yearEnd", endYear);
         return "";
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return switch (fieldAsString) {
            case "publisher" -> handlePublisher(term);
            case "queryString" -> handleQueryString(term);
            case "sortBy" -> handleSortBy(term);
            default -> super.handleOtherField(fieldAsString, term);
        };
    }

    private Optional<String> handlePublisher(String publisher) {
        if (!this.getJSONPayload().has("publisher")) {
            this.getJSONPayload().put("publisher", new JSONArray());
        }
        return Optional.of(this.getJSONPayload().getJSONArray("publisher").put(publisher).toString());
    }

    private Optional<String> handleQueryString(String queryString) {
        this.getJSONPayload().put("queryString", queryString);
        return Optional.empty();
    }

    private Optional<String> handleSortBy(String sortBy) {
        this.getJSONPayload().put("sortBy", sortBy);
        return Optional.empty();
    }

    private Optional<String> getPublisher() {
        return Objects.isNull(publisher) ? Optional.empty() : Optional.of(publisher.toString());
    }

    private Optional<String> getQueryString() {
        return Objects.isNull(queryString) ? Optional.empty() : Optional.of(queryString);
    }

    private Optional<String> getSortBy() {
        return Objects.isNull(sortBy) ? Optional.empty() : Optional.of(sortBy);
    }

    public JSONObject getJSONPayload() {
        return this.payload;
    }
}
