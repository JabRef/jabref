package org.jabref.model.search;

import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public enum LinkedFilesConstants {
    /// Version number for the search index.
    /// Increment when:
    ///
    /// 1. Index changes require reindexing (e.g., new/removed/renamed fields, analyzer changes)
    /// 2. Lucene codec changes (see module-info.java Lucene section)
    /// Incrementing triggers reindexing.
    VERSION("7"),
    PATH("path"),
    CONTENT("content"),
    ANNOTATIONS("annotations"),
    /// Case-preserving copy of [#CONTENT], used for case-sensitive searches (`=!`, `==!`, `=~!`).
    CONTENT_CASE_SENSITIVE("contentCaseSensitive"),
    /// Case-preserving copy of [#ANNOTATIONS], used for case-sensitive searches (`=!`, `==!`, `=~!`).
    ANNOTATIONS_CASE_SENSITIVE("annotationsCaseSensitive"),
    PAGE_NUMBER("pageNumber"),
    MODIFIED("modified");

    public static final List<String> PDF_FIELDS = List.of(CONTENT.toString(), ANNOTATIONS.toString());

    /// The case-preserving counterpart of each entry of [#PDF_FIELDS], in the same order.
    public static final List<String> CASE_SENSITIVE_PDF_FIELDS = List.of(CONTENT_CASE_SENSITIVE.toString(), ANNOTATIONS_CASE_SENSITIVE.toString());

    /// [EnglishAnalyzer] lowercases and stems, so the case of the indexed text is lost.
    /// The case-sensitive fields are therefore only tokenized - nothing else.
    private static final Analyzer CASE_PRESERVING_ANALYZER = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            return new TokenStreamComponents(new StandardTokenizer());
        }
    };

    public static final Analyzer LINKED_FILES_ANALYZER = new PerFieldAnalyzerWrapper(
            new EnglishAnalyzer(),
            Map.of(CONTENT_CASE_SENSITIVE.toString(), CASE_PRESERVING_ANALYZER,
                    ANNOTATIONS_CASE_SENSITIVE.toString(), CASE_PRESERVING_ANALYZER));

    private final String field;

    LinkedFilesConstants(String field) {
        this.field = field;
    }

    /// @return the case-preserving counterpart of `field` if there is one, `field` itself otherwise
    public static String caseSensitiveFieldOf(String field) {
        int index = PDF_FIELDS.indexOf(field);
        return index == -1 ? field : CASE_SENSITIVE_PDF_FIELDS.get(index);
    }

    @Override
    public String toString() {
        return field;
    }
}
