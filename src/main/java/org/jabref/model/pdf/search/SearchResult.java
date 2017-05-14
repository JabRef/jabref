package org.jabref.model.pdf.search;

import java.io.IOException;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

import static org.jabref.model.pdf.search.SearchFieldConstants.AUTHOR;
import static org.jabref.model.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.model.pdf.search.SearchFieldConstants.KEY;
import static org.jabref.model.pdf.search.SearchFieldConstants.KEYWORDS;
import static org.jabref.model.pdf.search.SearchFieldConstants.SUBJECT;
import static org.jabref.model.pdf.search.SearchFieldConstants.UID;

public final class SearchResult {

    private final String uid;
    private final String key;
    private final String content;
    private final String author;
    private final String subject;
    private final String keyword;

    private final float luceneScore;

    public SearchResult(IndexSearcher searcher, ScoreDoc scoreDoc) throws IOException {
        this.uid = getFieldContents(searcher, scoreDoc, UID);
        this.key = getFieldContents(searcher, scoreDoc, KEY);
        this.content = getFieldContents(searcher, scoreDoc, CONTENT);
        this.author = getFieldContents(searcher, scoreDoc, AUTHOR);
        this.subject = getFieldContents(searcher, scoreDoc, SUBJECT);
        this.keyword = getFieldContents(searcher, scoreDoc, KEYWORDS);
        this.luceneScore = scoreDoc.score;
    }

    private String getFieldContents(IndexSearcher searcher, ScoreDoc scoreDoc, String field) throws IOException {
        IndexableField indexableField = searcher.doc(scoreDoc.doc).getField(field);
        if (indexableField == null) {
            return "";
        }
        return indexableField.stringValue();
    }

    public String getUid() {
        return uid;
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
}
