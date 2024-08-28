package org.jabref.model.search;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public enum SearchFieldConstants {

    VERSION("99"),
    DEFAULT_FIELD("all"),
    ENTRY_ID("id"),
    ENTRY_TYPE("entrytype"),
    PATH("path"),
    CONTENT("content"),
    ANNOTATIONS("annotations"),
    PAGE_NUMBER("pageNumber"),
    MODIFIED("modified");

    public static final Analyzer Whitespace_ANALYZER = new WhitespaceAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    public static final Analyzer NGram_Analyzer_For_INDEXING = new NGramAnalyzer(1, Integer.MAX_VALUE, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    public static final List<String> PDF_FIELDS = List.of(PATH.toString(), CONTENT.toString(), ANNOTATIONS.toString());
    private final String field;

    SearchFieldConstants(String field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return field;
    }
}
