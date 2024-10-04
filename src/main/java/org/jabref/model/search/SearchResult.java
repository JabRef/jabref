package org.jabref.model.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.TextFragment;

public final class SearchResult {

    private final float searchScore;
    private final boolean hasFulltextResults;
    private final String path;
    private final String pageContent;
    private final String annotation;
    private final int pageNumber;
    private final Highlighter highlighter;
    private List<String> contentResultStringsHtml;
    private List<String> annotationsResultStringsHtml;

    private SearchResult(float searchScore,
                         boolean hasFulltextResults,
                         String path,
                         String pageContent,
                         String annotation,
                         int pageNumber,
                         Highlighter highlighter) {
        this.searchScore = searchScore;
        this.hasFulltextResults = hasFulltextResults;
        this.path = path;
        this.pageContent = pageContent;
        this.annotation = annotation;
        this.pageNumber = pageNumber;
        this.highlighter = highlighter;
    }

    public SearchResult(float searchScore) {
        this(searchScore, false, "", "", "", -1, null);
    }

    public SearchResult(float searchScore, String path, String pageContent, String annotation, int pageNumber, Highlighter highlighter) {
        this(searchScore, true, path, pageContent, annotation, pageNumber, highlighter);
    }

    public List<String> getContentResultStringsHtml() {
        if (contentResultStringsHtml == null) {
            return contentResultStringsHtml = getHighlighterFragments(highlighter, SearchFieldConstants.CONTENT, pageContent);
        }
        return contentResultStringsHtml;
    }

    public List<String> getAnnotationsResultStringsHtml() {
        if (annotationsResultStringsHtml == null) {
            annotationsResultStringsHtml = getHighlighterFragments(highlighter, SearchFieldConstants.ANNOTATIONS, annotation);
        }
        return annotationsResultStringsHtml;
    }

    public float getSearchScore() {
        return searchScore;
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

    private static List<String> getHighlighterFragments(Highlighter highlighter, SearchFieldConstants field, String content) {
        try (TokenStream contentStream = SearchFieldConstants.LINKED_FILES_ANALYZER.tokenStream(field.toString(), content)) {
            TextFragment[] frags = highlighter.getBestTextFragments(contentStream, content, true, 10);
            return Arrays.stream(frags).map(TextFragment::toString).toList();
        } catch (IOException | InvalidTokenOffsetsException e) {
            return List.of();
        }
    }
}
