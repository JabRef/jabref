package org.jabref.model.pdf.search;

import java.io.IOException;

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

import static org.jabref.model.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.model.pdf.search.SearchFieldConstants.MODIFIED;
import static org.jabref.model.pdf.search.SearchFieldConstants.PATH;

public final class SearchResult {

    private final String path;
    private final String content;
    private final long modified;

    private final float luceneScore;
    private String html;

    public SearchResult(IndexSearcher searcher, Query query, ScoreDoc scoreDoc) throws IOException {
        this.path = getFieldContents(searcher, scoreDoc, PATH);
        this.content = getFieldContents(searcher, scoreDoc, CONTENT);
        this.modified = Long.parseLong(getFieldContents(searcher, scoreDoc, MODIFIED));
        this.luceneScore = scoreDoc.score;

        TokenStream stream = new EnglishStemAnalyzer().tokenStream(CONTENT, content);

        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(query));
        try {

            TextFragment[] frags = highlighter.getBestTextFragments(stream, content, true, 10);
            this.html = "";
            for (TextFragment frag : frags) {
                html += "<p>" + frag.toString() + "</p>";
            }
        } catch (InvalidTokenOffsetsException e) {
            this.html = "";
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

    public String getContent() {
        return content;
    }

    public long getModified() {
        return modified;
    }

    public float getLuceneScore() {
        return luceneScore;
    }

    public String getHtml() {
        return html;
    }
}
