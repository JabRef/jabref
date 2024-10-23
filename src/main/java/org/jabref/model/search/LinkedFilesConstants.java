package org.jabref.model.search;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public enum LinkedFilesConstants {
    /**
     * Version number for the search index.
     * Increment when:
     * 1. Index changes require reindexing (e.g., new/removed/renamed fields, analyzer changes)
     * 2. Lucene codec changes (see module-info.java Lucene section)
     * Incrementing triggers reindexing.
     */
    VERSION("3"),
    PATH("path"),
    CONTENT("content"),
    ANNOTATIONS("annotations"),
    PAGE_NUMBER("pageNumber"),
    MODIFIED("modified");

    public static final Analyzer LINKED_FILES_ANALYZER = new EnglishAnalyzer();
    public static final List<String> PDF_FIELDS = List.of(CONTENT.toString(), ANNOTATIONS.toString());
    private final String field;

    LinkedFilesConstants(String field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return field;
    }
}
