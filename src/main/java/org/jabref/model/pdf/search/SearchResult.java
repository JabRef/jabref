package org.jabref.model.pdf.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
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
    private final boolean hasFulltextResults;

    private final float luceneScore;
    private List<String> contentResultStringsHtml = List.of();
    private List<String> annotationsResultStringsHtml = List.of();

    public SearchResult(IndexSearcher searcher, Query query, ScoreDoc scoreDoc) throws IOException {
        this.path = getFieldContents(searcher, scoreDoc, PATH);
        this.luceneScore = scoreDoc.score;
        if (this.path.length() > 0) {
            // pdf result
            this.pageNumber = Integer.parseInt(getFieldContents(searcher, scoreDoc, PAGE_NUMBER));
            this.modified = Long.parseLong(getFieldContents(searcher, scoreDoc, MODIFIED));
            this.hash = 0;

            String content = getFieldContents(searcher, scoreDoc, CONTENT);
            String annotations = getFieldContents(searcher, scoreDoc, ANNOTATIONS);
            this.hasFulltextResults = !(content.isEmpty() && annotations.isEmpty());

            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<b>", "</b>"), new QueryScorer(query));

            try (Analyzer analyzer = new EnglishAnalyzer();
             TokenStream contentStream = analyzer.tokenStream(CONTENT, content)) {
                TextFragment[] frags = highlighter.getBestTextFragments(contentStream, content, true, 10);
                this.contentResultStringsHtml = Arrays.stream(frags).map(TextFragment::toString).collect(Collectors.toList());
            } catch (InvalidTokenOffsetsException e) {
                this.contentResultStringsHtml = List.of();
            }

            try (Analyzer analyzer = new EnglishAnalyzer();
             TokenStream annotationStream = analyzer.tokenStream(ANNOTATIONS, annotations)) {
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
            this.hasFulltextResults = false;
        }
    }

    public List<BibEntry> getMatchingEntries(BibDatabaseContext databaseContext) {
        if (this.path.length() > 0) {
            return getEntriesWithFile(path, databaseContext);
        }
        return databaseContext.getEntries().stream().filter(bibEntry -> bibEntry.getLastIndexHash() == this.hash).collect(Collectors.toList());
    }

    private List<BibEntry> getEntriesWithFile(String path, BibDatabaseContext databaseContext) {
        return databaseContext.getEntries().stream().filter(entry -> entry.getFiles().stream().map(LinkedFile::getLink).anyMatch(link -> link.equals(path))).collect(Collectors.toList());
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

    public boolean hasFulltextResults() {
        return this.hasFulltextResults;
    }
}
