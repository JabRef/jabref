package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jabref.logic.formatter.casechanger.Word;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ISIDOREQueryTransformer extends YearRangeByFilteringQueryTransformer {

    public static final int MAX_TERMS = 4;
    private static final Logger LOGGER = LoggerFactory.getLogger(ISIDOREQueryTransformer.class);

    private int handleCountTitle = 0;
    private int handleCountAuthor = 0;

    private int handleUnfieldedTermCount = 0;

    @Override
    protected String getLogicalAndOperator() {
        return "+AND+";
    }

    @Override
    protected String getLogicalOrOperator() {
        return "+OR+";
    }

    @Override
    protected String getLogicalNotOperator() {
        return "+NOT+";
    }

    @Override
    protected String handleAuthor(String author) {
        handleCountAuthor++;
        if (handleCountAuthor > 3) {
            return "";
        }
        return createKeyValuePair("author", author.replace(" ", "_"), "=");
    }

    @Override
    protected String handleTitle(String title) {
        handleCountTitle++;
        if (handleCountTitle > 3) {
            return "";
        }
        return createKeyValuePair("title", StringUtil.quoteStringIfSpaceIsContained(title));
    }

    @Override
    protected String handleJournal(String journalTitle) {
        LOGGER.warn("ISIDORE does not support searching by journal");
        return handleTitle(journalTitle);
    }

    @Override
    protected String handleYear(String year) {
        return createKeyValuePair("date", year, "=");
    }

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        if (Word.SMALLER_WORDS.contains(term)) {
            return Optional.empty();
        }
        handleUnfieldedTermCount++;
        if (handleUnfieldedTermCount > MAX_TERMS) {
            return Optional.empty();
        }
        return super.handleUnFieldedTerm(term);
    }
}
