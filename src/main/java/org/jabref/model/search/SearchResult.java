package org.jabref.model.search;

import java.util.List;

public final class SearchResult {

    private final float luceneScore;
    private final boolean hasFulltextResults;
    private final String path;
    private final int pageNumber;
    private final long modified;
    private final List<String> contentResultStringsHtml;
    private final List<String> annotationsResultStringsHtml;
    private final List<String> bibEntryStringsHtml;

    public SearchResult(float luceneScore, List<String> bibEntryStringsHtml) {
        this(luceneScore,
                false,
                "",
                -1,
                -1,
                bibEntryStringsHtml,
                List.of(),
                List.of());
    }

    public SearchResult(float luceneScore,
                        String path,
                        int pageNumber,
                        long modified,
                        List<String> contentResultStringsHtml,
                        List<String> annotationsResultStringsHtml) {
        this(luceneScore,
                true,
                path,
                pageNumber,
                modified,
                List.of(),
                contentResultStringsHtml,
                annotationsResultStringsHtml);
    }

    private SearchResult(float luceneScore,
                        boolean hasFulltextResults,
                        String path,
                        int pageNumber,
                        long modified,
                        List<String> bibEntryStringsHtml,
                        List<String> contentResultStringsHtml,
                        List<String> annotationsResultStringsHtml) {
        this.luceneScore = luceneScore;
        this.hasFulltextResults = hasFulltextResults;
        this.path = path;
        this.pageNumber = pageNumber;
        this.modified = modified;
        this.bibEntryStringsHtml = bibEntryStringsHtml;
        this.contentResultStringsHtml = contentResultStringsHtml;
        this.annotationsResultStringsHtml = annotationsResultStringsHtml;
    }

    public float getLuceneScore() {
        return luceneScore;
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

    public long getModified() {
        return modified;
    }

    public List<String> getContentResultStringsHtml() {
        return contentResultStringsHtml;
    }

    public List<String> getAnnotationsResultStringsHtml() {
        return annotationsResultStringsHtml;
    }

    public List<String> getBibEntryStringsHtml() {
        return bibEntryStringsHtml;
    }
}
