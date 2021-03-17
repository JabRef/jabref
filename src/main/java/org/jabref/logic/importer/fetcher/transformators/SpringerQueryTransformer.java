package org.jabref.logic.importer.fetcher.transformators;

import java.util.StringJoiner;

/**
 * This class converts a query string written in lucene syntax into a complex  query.
 *
 * For simplicity this is currently limited to fielded data and the boolean AND operator.
 */
public class SpringerQueryTransformer extends AbstractQueryTransformer {

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
        return String.format("name:\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("title:\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return String.format("journal:\"%s\"", journalTitle);

    }

    @Override
    protected String handleYear(String year) {
        return String.format("date:%s*", year);
    }

    @Override
    protected String handleYearRange(String yearRange) {
        String[] split = yearRange.split("-");
        StringJoiner resultBuilder = new StringJoiner("*" + getLogicalOrOperator() + "date:", "(date:", "*)");
        for (int i = Integer.parseInt(split[0]); i <= Integer.parseInt(split[1]); i++) {
            resultBuilder.add(String.valueOf(i));
        }
        return resultBuilder.toString();
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return "\"" + term + "\"";
    }
}
