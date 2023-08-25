package org.jabref.logic.importer.fetcher.transformers;

import java.util.Calendar;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class CiteSeerQueryTransformer extends AbstractQueryTransformer {

    private JSONObject payload = new JSONObject();

    /**
     * Default values for necessary parameters set in constructor
     */
    public CiteSeerQueryTransformer() {
        handlePage("1");
        handlePageSize("20");
        this.getJSONPayload().put("must_have_pdf", "false");
        handleSortBy("relevance");
    }

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
        return "";
    }

    @Override
    protected String handleAuthor(String author) {
        if (!getJSONPayload().has("author")) {
            this.getJSONPayload().put("author", new JSONArray());
        }
        this.getJSONPayload().getJSONArray("author").put(author).toString();
        return StringUtil.quoteStringIfSpaceIsContained(author);
    }

    @Override
    protected String handleTitle(String title) {
        this.getJSONPayload().put("queryString", title);
        return StringUtil.quoteStringIfSpaceIsContained(title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        this.getJSONPayload().put("journal", journalTitle);
        return StringUtil.quoteStringIfSpaceIsContained(journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        this.getJSONPayload().put("yearStart", Integer.parseInt(year));
        this.getJSONPayload().put("yearEnd", Integer.parseInt(year));
        return StringUtil.quoteStringIfSpaceIsContained(year);
    }

    @Override
    protected String handleYearRange(String yearRange) {
         parseYearRange(yearRange);
         if (endYear == Integer.MAX_VALUE) { // invalid year range
             Calendar calendar = Calendar.getInstance();
             this.getJSONPayload().put("yearEnd", calendar.get(Calendar.YEAR));
             return "";
         }
         this.getJSONPayload().put("yearStart", startYear);
         this.getJSONPayload().put("yearEnd", endYear);
         return yearRange;
    }

    /**
     * covers the five fields that are required to make a POST request
     * except "must_have_pdf" as FullTextFetcher is not yet implemented for CiteSeer
     */
    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return switch (fieldAsString) {
            case "page" -> handlePage(term);
            case "pageSize" -> handlePageSize(term);
            case "must_have_pdf" -> handleMustHavePdf(term);
            case "sortBy" -> handleSortBy(term);
            default -> super.handleOtherField(fieldAsString, term);
        };
    }

    // as mentioned before, there may be a Jabref constant for page/page-size
    private Optional<String> handlePage(String page) {
        this.getJSONPayload().put("page", StringUtil.intValueOf(page));
        return Optional.of(page);
    }

    private Optional<String> handlePageSize(String pageSize) {
        this.getJSONPayload().put("pageSize", StringUtil.intValueOf(pageSize));
        return Optional.of(pageSize);
    }

    private Optional<String> handleMustHavePdf(String mustHavePdf) {
        this.getJSONPayload().put("must_have_pdf", mustHavePdf);
        return Optional.of(mustHavePdf);
    }

    private Optional<String> handleSortBy(String sortBy) {
        this.getJSONPayload().put("sortBy", sortBy);
        return Optional.of(sortBy);
    }

    public JSONObject getJSONPayload() {
        return this.payload;
    }
}
