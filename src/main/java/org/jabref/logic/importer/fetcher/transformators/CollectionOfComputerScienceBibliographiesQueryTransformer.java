package org.jabref.logic.importer.fetcher.transformators;

public class CollectionOfComputerScienceBibliographiesQueryTransformer extends AbstractQueryTransformer {

    @Override
    protected String getLogicalAndOperator() {
        return " ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " OR ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "-";
    }

    @Override
    protected String handleAuthor(String author) {
        return String.format("au:\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        return String.format("ti:\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return String.format("\"%s\"", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return String.format("year:%s", year);
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        if (term.contains(" ")) {
            return String.format("\"%s\"", term);
        } else {
            return term;
        }
    }
}
