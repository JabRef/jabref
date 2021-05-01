package org.jabref.logic.importer.fetcher.transformers;

import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBLPQueryTransformer extends AbstractQueryTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBLPQueryTransformer.class);

    @Override
    protected String getLogicalAndOperator() {
        return " ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return "|";
    }

    @Override
    protected String getLogicalNotOperator() {
        LOGGER.warn("DBLP does not support Boolean NOT operator.");
        return "";
    }

    @Override
    protected String handleAuthor(String author) {
        // DBLP does not support explicit author field search
        return String.format("\"%s\"", author);
    }

    @Override
    protected String handleTitle(String title) {
        // DBLP does not support explicit title field search
        return String.format("\"%s\"", title);
    }

    @Override
    protected String handleJournal(String journalTitle) {
        // DBLP does not support explicit journal field search
        return String.format("\"%s\"", journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        // DBLP does not support explicit year field search
        return year;
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return String.format("\"%s\"", term);
    }
}
