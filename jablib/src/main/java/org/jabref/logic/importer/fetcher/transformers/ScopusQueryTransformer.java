package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

/**
 * Query transformer for the Scopus Search API.
 * <p>
 * Scopus uses specific field codes for advanced search:
 * - TITLE-ABS-KEY-AUTH() for searching in title, abstract, author and keywords
 * - AUTH() for author search
 * - SRCTITLE() for journal/source title search
 * - PUBYEAR for publication year
 * - DOI() for DOI search
 * <p>
 * Scopus supports AND, OR, AND NOT as boolean operators.
 * <p>
 * See: https://dev.elsevier.com/sc_search_tips.html
 */
public class ScopusQueryTransformer extends YearRangeByFilteringQueryTransformer {

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
        return "AND NOT ";
    }

    @Override
    protected String handleAuthor(String author) {
        return "AUTH(" + author + ")";
    }

    @Override
    protected String handleTitle(String title) {
        return "TITLE(" + title + ")";
    }

    @Override
    protected String handleJournal(String journalTitle) {
        return "SRCTITLE(" + journalTitle + ")";
    }

    @Override
    protected String handleYear(String year) {
        try {
            int yearInt = Integer.parseInt(year);
            startYear = Math.min(startYear, yearInt);
            endYear = Math.max(endYear, yearInt);
        } catch (NumberFormatException e) {
            // Ignore invalid year
        }
        return "PUBYEAR = " + year;
    }

    @Override
    protected String handleYearRange(String yearRange) {
        parseYearRange(yearRange);
        if (startYear != Integer.MAX_VALUE && endYear != Integer.MIN_VALUE) {
            return "PUBYEAR > " + (startYear - 1) + " AND PUBYEAR < " + (endYear + 1);
        }
        return "";
    }

    @Override
    protected String handleDoi(String doi) {
        return "DOI(" + doi + ")";
    }

    @Override
    protected Optional<String> handleUnFieldedTerm(String term) {
        // For unfielded terms, use TITLE-ABS-KEY-AUTH which searches in title, abstract, author and keywords
        if (term.contains(" ")) {
            return Optional.of("TITLE-ABS-KEY-AUTH(\"" + term + "\")");
        }
        return Optional.of("TITLE-ABS-KEY-AUTH(" + term + ")");
    }

    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return switch (fieldAsString.toLowerCase()) {
            case "abstract" ->
                    Optional.of("ABS(" + term + ")");
            case "keywords" ->
                    Optional.of("KEY(" + term + ")");
            case "affiliation" ->
                    Optional.of("AFFIL(" + term + ")");
            case "issn" ->
                    Optional.of("ISSN(" + term + ")");
            case "isbn" ->
                    Optional.of("ISBN(" + term + ")");
            default ->
                    super.handleOtherField(fieldAsString, term);
        };
    }
}
