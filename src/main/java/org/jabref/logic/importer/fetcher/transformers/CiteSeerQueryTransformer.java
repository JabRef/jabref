package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class CiteSeerQueryTransformer extends AbstractQueryTransformer {

    private JSONObject payload = new JSONObject();

    @Override
    protected String getLogicalAndOperator() {
        return " AND ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " OR ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return " NOT ";
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

    // the five fields are required to make a POST request
    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return switch (fieldAsString) {
            case "queryString" -> handleQueryString(term);
            case "page" -> handlePage(term);
            case "pageSize" -> handlePageSize(term);
            case "must_have_pdf" -> handleMustHavePdf(term);
            case "sortBy" -> handleSortBy(term);
            default -> super.handleOtherField(fieldAsString, term);
        };
    }

    private Optional<String> handleQueryString(String queryString) {
        this.getJSONPayload().put("queryString", queryString);
        return Optional.empty();
    }

    // as mentioned before, there may be a Jabref constant for page/page-size
    private Optional<String> handlePage(String page) {
        this.getJSONPayload().put("page", page);
        return Optional.empty();
    }

    private Optional<String> handlePageSize(String pageSize) {
        this.getJSONPayload().put("pageSize", pageSize);
        return Optional.empty();
    }

    private Optional<String> handleMustHavePdf(String mustHavePdf) {
        this.getJSONPayload().put("must_have_pdf", mustHavePdf);
        return Optional.empty();
    }

    private Optional<String> handleSortBy(String sortBy) {
        this.getJSONPayload().put("sortBy", sortBy);
        return Optional.empty();
    }

    public JSONObject getJSONPayload() {
        return this.payload;
    }
}
