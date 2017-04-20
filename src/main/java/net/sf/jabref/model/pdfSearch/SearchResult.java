package net.sf.jabref.model.pdfSearch;

import net.sf.jabref.logic.search.PDFSearch.SearchFieldConstants;

/**
 * Created by christoph on 04.08.16.
 */
public class SearchResult {

    private String key;
    private String content;
    private String author;
    private String creator;
    private String subject;
    private String keyword;

    private float luceneScore;

    public SearchResult() {

    }

    public SearchResult(String key, String content, String author, String creator, String subject, String keyword, float luceneScore) {
        this.key = key;
        this.content = content;
        this.author = author;
        this.creator = creator;
        this.subject = subject;
        this.keyword = keyword;
        this.luceneScore = luceneScore;
    }

    public void mapField(String fieldName, String value) {

        switch (fieldName) {
            case SearchFieldConstants.KEY:
                setKey(value);
                break;
            case SearchFieldConstants.AUTHOR:
                setAuthor(value);
                break;
            case SearchFieldConstants.CREATOR:
                setCreator(value);
                break;
            case SearchFieldConstants.SUBJECT:
                setSubject(value);
                break;
            case SearchFieldConstants.CONTENT:
                setContent(value);
                break;
            case SearchFieldConstants.KEYWORDS:
                setKeyword(value);
                break;
            default:
                break;

        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public float getLuceneScore() {
        return luceneScore;
    }

    public void setLuceneScore(float luceneScore) {
        this.luceneScore = luceneScore;
    }
}
