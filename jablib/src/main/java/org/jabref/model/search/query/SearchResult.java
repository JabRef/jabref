package org.jabref.model.search.query;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.search.LinkedFilesConstants;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;

public final class SearchResult {

    private final boolean hasFulltextResults;
    private final String path;
    private final String pageContent;
    private final String annotation;
    private final int pageNumber;
    private final Query query;
    private List<String> contentResultStringsHtml;
    private List<String> annotationsResultStringsHtml;

    private SearchResult(boolean hasFulltextResults,
                         String path,
                         String pageContent,
                         String annotation,
                         int pageNumber,
                         Query query) {
        this.hasFulltextResults = hasFulltextResults;
        this.path = path;
        this.pageContent = pageContent;
        this.annotation = annotation;
        this.pageNumber = pageNumber;
        this.query = query;
    }

    public SearchResult() {
        this(false, "", "", "", -1, null);
    }

    public SearchResult(String path, String pageContent, String annotation, int pageNumber, Query query) {
        this(true, path, pageContent, annotation, pageNumber, query);
    }

    public List<String> getContentResultStringsHtml() {
        if (contentResultStringsHtml == null) {
            return contentResultStringsHtml = getHighlighterFragments(query, LinkedFilesConstants.CONTENT, pageContent);
        }
        return contentResultStringsHtml;
    }

    public List<String> getAnnotationsResultStringsHtml() {
        if (annotationsResultStringsHtml == null) {
            annotationsResultStringsHtml = getHighlighterFragments(query, LinkedFilesConstants.ANNOTATIONS, annotation);
        }
        return annotationsResultStringsHtml;
    }

    public boolean hasFulltextResults() {
        return hasFulltextResults;
    }

    public String getPath() {
        return path;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    /// Each field is highlighted only with the terms the query has for exactly that field, tokenized by that field's analyzer.
    /// Otherwise, the lowercasing analyzer of `field` would also mark differently cased occurrences of a case-sensitive term.
    private static List<String> getHighlighterFragments(Query query, LinkedFilesConstants field, String content) {
        return Stream.of(field.toString(), LinkedFilesConstants.caseSensitiveFieldOf(field.toString()))
                     .flatMap(fieldName -> getHighlighterFragments(query, fieldName, content).stream())
                     .distinct()
                     .toList();
    }

    private static List<String> getHighlighterFragments(Query query, String field, String content) {
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(query, field));
        try (TokenStream contentStream = LinkedFilesConstants.LINKED_FILES_ANALYZER.tokenStream(field, content)) {
            TextFragment[] frags = highlighter.getBestTextFragments(contentStream, content, true, 10);
            return Arrays.stream(frags).filter(frag -> frag.getScore() > 0).map(TextFragment::toString).toList();
        } catch (IOException | InvalidTokenOffsetsException _) {
            return List.of();
        }
    }
}
