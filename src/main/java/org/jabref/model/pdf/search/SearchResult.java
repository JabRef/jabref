package org.jabref.model.pdf.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;

import static org.jabref.model.pdf.search.SearchFieldConstants.ANNOTATIONS;
import static org.jabref.model.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.model.pdf.search.SearchFieldConstants.MODIFIED;
import static org.jabref.model.pdf.search.SearchFieldConstants.PAGE_NUMBER;
import static org.jabref.model.pdf.search.SearchFieldConstants.PATH;

public final class SearchResult {

    private final String path;

    private final int pageNumber;
    private final long modified;

    private final float luceneScore;
    private List<String> contentResultStringsHtml;
    private List<String> annotationsResultStringsHtml;

    public SearchResult(IndexSearcher searcher, Query query, ScoreDoc scoreDoc) throws IOException {
        this.path = getFieldContents(searcher, scoreDoc, PATH);
        this.pageNumber = Integer.parseInt(getFieldContents(searcher, scoreDoc, PAGE_NUMBER));
        this.modified = Long.parseLong(getFieldContents(searcher, scoreDoc, MODIFIED));
        this.luceneScore = scoreDoc.score;

        String content = getFieldContents(searcher, scoreDoc, CONTENT);
        String annotations = getFieldContents(searcher, scoreDoc, ANNOTATIONS);

        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(query));

        try (TokenStream contentStream = new EnglishStemAnalyzer().tokenStream(CONTENT, content)) {
            TextFragment[] frags = highlighter.getBestTextFragments(contentStream, content, true, 10);
            this.contentResultStringsHtml = Arrays.stream(frags).map(TextFragment::toString).collect(Collectors.toList());
        } catch (InvalidTokenOffsetsException e) {
            this.contentResultStringsHtml = List.of();
        }

        try (TokenStream annotationStream = new EnglishStemAnalyzer().tokenStream(ANNOTATIONS, annotations)) {
            TextFragment[] frags = highlighter.getBestTextFragments(annotationStream, annotations, true, 10);
            this.annotationsResultStringsHtml = Arrays.stream(frags).map(TextFragment::toString).collect(Collectors.toList());
        } catch (InvalidTokenOffsetsException e) {
            this.annotationsResultStringsHtml = List.of();
        }
    }

    private String getFieldContents(IndexSearcher searcher, ScoreDoc scoreDoc, String field) throws IOException {
        IndexableField indexableField = searcher.doc(scoreDoc.doc).getField(field);
        if (indexableField == null) {
            return "";
        }
        return indexableField.stringValue();
    }

    public boolean isResultFor(BibEntry entry) {
        return entry.getFiles().stream().anyMatch(linkedFile -> path.equals(linkedFile.getLink()));
    }

    public String getPath() {
        return path;
    }

    public long getModified() {
        return modified;
    }

    public float getLuceneScore() {
        return luceneScore;
    }

    public List<String> getContentResultStringsHtml() {
        return contentResultStringsHtml;
    }

    public List<String> getAnnotationsResultStringsHtml() {
        return annotationsResultStringsHtml;
    }

    public int getPageNumber() {
        return pageNumber;
    }
}
