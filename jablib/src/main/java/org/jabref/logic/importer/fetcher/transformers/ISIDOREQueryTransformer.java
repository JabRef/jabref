package org.jabref.logic.importer.fetcher.transformers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.formatter.casechanger.Word;
import org.jabref.model.entry.AuthorList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ISIDOREQueryTransformer extends YearRangeByFilteringQueryTransformer {

    public static final int MAX_TERMS = 4;
    private static final Logger LOGGER = LoggerFactory.getLogger(ISIDOREQueryTransformer.class);

    private int handleCountTitle = 0;
    private int handleCountAuthor = 0;
    private int handleUnfieldedTermCount = 0;

    private Map<String, String> parameterMap = new HashMap<>();

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
        String lastFirst = AuthorList.fixAuthorLastNameFirstCommas(author, false, true);
        lastFirst = lastFirst.replace(", ", "_").toLowerCase(Locale.ENGLISH);
        parameterMap.put("author", lastFirst);
        return "";
    }

    @Override
    protected String handleTitle(String title) {
        handleCountTitle++;
        if (handleCountTitle > 3) {
            return "";
        }
        return handleUnFieldedTerm(title).orElse("");
    }

    @Override
    protected String handleJournal(String journalTitle) {
        LOGGER.warn("ISIDORE does not support searching by journal");
        return handleUnFieldedTerm(journalTitle).orElse("");
    }

    @Override
    protected String handleYear(String year) {
        parameterMap.put("date", year);
        return "";
    }

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        if (Word.SMALLER_WORDS.contains(term)) {
            return Optional.empty();
        }

        // if not letter or digit, ignore
        if (!term.matches("\\w+")) {
            return Optional.empty();
        }

        handleUnfieldedTermCount++;
        if (handleUnfieldedTermCount > MAX_TERMS) {
            return Optional.empty();
        }
        return super.handleUnFieldedTerm(term);
    }

    public Map<String, String> getParameterMap() {
        return this.parameterMap;
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        Optional<String> first = handleUnFieldedTerm(fieldAsString);
        Optional<String> second = handleUnFieldedTerm(term);
        if (first.isEmpty() && second.isEmpty()) {
            return Optional.empty();
        }
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return Optional.of(fieldAsString + " " + term);
    }
}
