package org.jabref.model.pdf.search;

import java.io.IOException;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

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
import org.apache.lucene.search.highlight.TokenSources;

import static org.jabref.model.pdf.search.SearchFieldConstants.AUTHOR;
import static org.jabref.model.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.model.pdf.search.SearchFieldConstants.KEY;
import static org.jabref.model.pdf.search.SearchFieldConstants.KEYWORDS;
import static org.jabref.model.pdf.search.SearchFieldConstants.PATH;
import static org.jabref.model.pdf.search.SearchFieldConstants.SUBJECT;

public final class SearchResult {

    private final String path;
    private final String key;
    private final String content;
    private final String author;
    private final String subject;
    private final String keyword;

    private final float luceneScore;
    private String html;

    public SearchResult(IndexSearcher searcher, Query query, ScoreDoc scoreDoc) throws IOException {
        this.path = getFieldContents(searcher, scoreDoc, PATH);
        this.key = getFieldContents(searcher, scoreDoc, KEY);
        this.content = getFieldContents(searcher, scoreDoc, CONTENT);
        this.author = getFieldContents(searcher, scoreDoc, AUTHOR);
        this.subject = getFieldContents(searcher, scoreDoc, SUBJECT);
        this.keyword = getFieldContents(searcher, scoreDoc, KEYWORDS);
        this.luceneScore = scoreDoc.score;

        TokenStream stream = TokenSources.getTokenStream(CONTENT, content, new EnglishStemAnalyzer());
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
        for (LinkedFile linkedFile : entry.getFiles()) {
            if (this.path.equals(linkedFile.getLink())) {
                return true;
            }
        }
        return false;
    }

    public String getPath() {
        return path;
    }

    public String getKey() {
        return key;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public String getSubject() {
        return subject;
    }

    public String getKeyword() {
        return keyword;
    }

    public float getLuceneScore() {
        return luceneScore;
    }

    public String getHtml() {
        return html;
    }
}
