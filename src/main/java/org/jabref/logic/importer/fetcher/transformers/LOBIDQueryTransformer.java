package org.jabref.logic.importer.fetcher.transformers;

public class LOBIDQueryTransformer extends AbstractQueryTransformer {

    @Override
    public String getLogicalAndOperator() {
        return " AND ";
    }

    @Override
    public String getLogicalOrOperator() {
        return " OR ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "-";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("contribution.agent.label", author);
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("title", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return createKeyValuePair("bibliographicCitation", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return "publication.startDate:[" + year + " TO " + year + "]";
    }

    @Override
    protected String handleYearRange(String yearRange) {
        parseYearRange(yearRange);
        if (endYear == Integer.MAX_VALUE) {
            return yearRange;
        }
        return "publication.startDate:[" + startYear + " TO " + endYear + "]";
    }
}
