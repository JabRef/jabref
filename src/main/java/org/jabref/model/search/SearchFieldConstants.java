package org.jabref.model.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class SearchFieldConstants {
    public static final String VERSION = "99";
    public static final String FILE_FIELDS_PREFIX = "f_";
    public static final String PATH = FILE_FIELDS_PREFIX + "path";
    public static final String CONTENT = FILE_FIELDS_PREFIX + "content";
    public static final String ANNOTATIONS = FILE_FIELDS_PREFIX + "annotations";
    public static final String PAGE_NUMBER = FILE_FIELDS_PREFIX + "pageNumber";
    public static final String MODIFIED = FILE_FIELDS_PREFIX + "modified";
    public static final String DEFAULT_FIELD = "default_field";
    public static final String BIB_ENTRY_ID = "bib_id";
    public static final String BIB_ENTRY_TYPE = "entrytype";
    public static final Analyzer ANALYZER = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
    public static final String[] PDF_FIELDS = {PATH, CONTENT, ANNOTATIONS};
    public static final Set<String> SEARCHABLE_BIB_FIELDS = new HashSet<>(List.of(BIB_ENTRY_ID, BIB_ENTRY_TYPE));
}
