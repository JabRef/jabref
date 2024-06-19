package org.jabref.model.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.apache.lucene.analysis.Analyzer;
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

public final class SearchResult {

    private final String path;
    private final int pageNumber;
    private final long modified;
    private final String entryId;
    private final boolean hasFulltextResults;
    private final float luceneScore;
    private List<String> contentResultStringsHtml = List.of();
    private List<String> annotationsResultStringsHtml = List.of();

    public SearchResult(IndexSearcher searcher, Query query, ScoreDoc scoreDoc) throws IOException {
        this.path = getFieldContents(searcher, scoreDoc, SearchFieldConstants.PATH);
        this.luceneScore = scoreDoc.score;
        if (!this.path.isEmpty()) {
            this.entryId = "";
            this.pageNumber = Integer.parseInt(getFieldContents(searcher, scoreDoc, SearchFieldConstants.PAGE_NUMBER));
            this.modified = Long.parseLong(getFieldContents(searcher, scoreDoc, SearchFieldConstants.MODIFIED));

            String content = getFieldContents(searcher, scoreDoc, SearchFieldConstants.CONTENT);
            String annotations = getFieldContents(searcher, scoreDoc, SearchFieldConstants.ANNOTATIONS);
            this.hasFulltextResults = !(content.isEmpty() && annotations.isEmpty());

            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(query));

            Analyzer analyzer = SearchFieldConstants.ANALYZER;
            try {
                try (TokenStream contentStream = analyzer.tokenStream(SearchFieldConstants.CONTENT, content)) {
                    TextFragment[] frags = highlighter.getBestTextFragments(contentStream, content, true, 10);
                    this.contentResultStringsHtml = Arrays.stream(frags).map(TextFragment::toString).toList();
                }
            } catch (InvalidTokenOffsetsException e) {
                this.contentResultStringsHtml = List.of();
            }

            try (TokenStream annotationStream = analyzer.tokenStream(SearchFieldConstants.ANNOTATIONS, annotations)) {
                TextFragment[] frags = highlighter.getBestTextFragments(annotationStream, annotations, true, 10);
                this.annotationsResultStringsHtml = Arrays.stream(frags).map(TextFragment::toString).toList();
            } catch (InvalidTokenOffsetsException e) {
                this.annotationsResultStringsHtml = List.of();
            }
        } else {
            this.entryId = getFieldContents(searcher, scoreDoc, SearchFieldConstants.BIB_ENTRY_ID);
            this.pageNumber = -1;
            this.modified = -1;
            this.hasFulltextResults = false;
        }
    }

    public List<BibEntry> getMatchingEntries(BibDatabaseContext databaseContext) {
        if (!this.path.isEmpty()) {
            return getEntriesWithFile(path, databaseContext);
        }
        return databaseContext.getEntries().stream().filter(bibEntry -> bibEntry.getId().equals(entryId)).toList();
    }

    private List<BibEntry> getEntriesWithFile(String path, BibDatabaseContext databaseContext) {
        return databaseContext.getEntries()
                              .stream()
                              .filter(entry ->
                                      entry.getFiles()
                                           .stream()
                                           .map(LinkedFile::getLink)
                                           .anyMatch(link -> link.equals(path)))
                              .toList();
    }

    private String getFieldContents(IndexSearcher searcher, ScoreDoc scoreDoc, String field) throws IOException {
        IndexableField indexableField = searcher.storedFields().document(scoreDoc.doc).getField(field);
        if (indexableField == null) {
            return "";
        }
        return indexableField.stringValue();
    }

    public float getSearchScoreFor(BibEntry entry) {
        if (this.path != null) {
            return entry.getFiles().stream().anyMatch(linkedFile -> path.equals(linkedFile.getLink())) ? luceneScore : 0;
        }
        return entry.getId().equals(entryId) ? luceneScore : 0;
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

    public boolean hasFulltextResults() {
        return this.hasFulltextResults;
    }
}
