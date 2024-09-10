package org.jabref.model.search;

import java.util.List;

import org.jabref.model.search.Analyzer.LatexAwareAnalyzer;
import org.jabref.model.search.Analyzer.LatexAwareNGramAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public enum SearchFieldConstants {
    /**
     * Version number for the search index.
     * Increment when:
     * 1. Index changes require reindexing (e.g., new/removed/renamed fields, analyzer changes)
     * 2. Lucene codec changes (see module-info.java Lucene section)
     * Incrementing triggers reindexing.
     */
    VERSION("1"),
    DEFAULT_FIELD("any"),
    ENTRY_ID("id"),
    ENTRY_TYPE("entrytype"),
    PATH("path"),
    CONTENT("content"),
    ANNOTATIONS("annotations"),
    PAGE_NUMBER("pageNumber"),
    MODIFIED("modified");

    public static final Analyzer LINKED_FILES_ANALYZER = new EnglishAnalyzer();
    public static final Analyzer LATEX_AWARE_ANALYZER = new LatexAwareAnalyzer();
    public static final Analyzer LATEX_AWARE_NGRAM_ANALYZER = new LatexAwareNGramAnalyzer(1, Integer.MAX_VALUE);
    public static final List<String> PDF_FIELDS = List.of(CONTENT.toString(), ANNOTATIONS.toString());
    private final String field;

    SearchFieldConstants(String field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return field;
    }
}
