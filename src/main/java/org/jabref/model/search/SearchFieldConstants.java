package org.jabref.model.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

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

    public static final Analyzer ANALYZER = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    public static final SearchFieldConstants[] PDF_FIELDS = {PATH, CONTENT, ANNOTATIONS};
    public static final Set<String> SEARCHABLE_BIB_FIELDS = new HashSet<>(List.of(ENTRY_ID.toString(), ENTRY_TYPE.toString()));

    private final String field;

    SearchFieldConstants(String field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return field;
    }
}
