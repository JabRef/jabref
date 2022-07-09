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
import static org.jabref.model.pdf.search.SearchFieldConstants.BIB_ENTRY_ID_HASH;
import static org.jabref.model.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.model.pdf.search.SearchFieldConstants.MODIFIED;
import static org.jabref.model.pdf.search.SearchFieldConstants.PAGE_NUMBER;
import static org.jabref.model.pdf.search.SearchFieldConstants.PATH;

public final class SearchResult {

    private final String path;

    private final int pageNumber;
    private final long modified;
    private final int hash;

    private final float luceneScore;
    private List<String> contentResultStringsHtml;
    private List<String> annotationsResultStringsHtml;

    public SearchResult(IndexSearcher searcher, Query query, ScoreDoc scoreDoc) throws IOException {
        this.luceneScore = scoreDoc.score;
        this.path = getFieldContents(searcher, scoreDoc, PATH);
        if (this.path.length() > 0) {
            // pdf result
            this.pageNumber = Integer.parseInt(getFieldContents(searcher, scoreDoc, PAGE_NUMBER));
            this.modified = Long.parseLong(getFieldContents(searcher, scoreDoc, MODIFIED));
            this.hash = 0;

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
        } else {
            // Found somewhere in the bib entry
            this.hash = Integer.parseInt(getFieldContents(searcher, scoreDoc, BIB_ENTRY_ID_HASH));
            this.pageNumber = -1;
            this.modified = -1;
        }
    }

    private String getFieldContents(IndexSearcher searcher, ScoreDoc scoreDoc, String field) throws IOException {
        IndexableField indexableField = searcher.doc(scoreDoc.doc).getField(field);
        if (indexableField == null) {
            return "";
        }
        return indexableField.stringValue();
    }

    public float getSearchScoreFor(BibEntry entry) {
        if (this.path != null) {
            return entry.getFiles().stream().anyMatch(linkedFile -> path.equals(linkedFile.getLink())) ? luceneScore : 0;
        }
        return entry.getLastIndexHash() == hash ? luceneScore : 0;
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
