package net.sf.jabref.model.pdfSearch;

import java.util.List;

import net.sf.jabref.logic.search.PDFSearch.SearchFieldConstants;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.TopDocCollector;

/**
 * Created by christoph on 04.08.16.
 */
public class ResultSet {

    private List<SearchResult> searchResults;
    private TopDocCollector collector;

    public ResultSet(Document[] documents, TopDocCollector collector) {
        this.searchResults = mapToSearchResults(documents);
        this.collector = collector;
    }

    private void sortByHits() {
        //TODO implement sorting
    }

    private void sortByAlphabet() {
        //TODO implement sorting
    }

    /**
     * Maps a lucene documents fields to a search result list
     *
     * @param documents a list of lucene documents with some fields set
     * @return
     */
    private List<SearchResult> mapToSearchResults(Document[] documents) {

        for (int i = 0; i < documents.length; i++) {
            SearchResult result = new SearchResult();
            for (String field : SearchFieldConstants.PDF_FIELDS) {
                result.mapField(field, documents[i].getField(field).stringValue());
            }
            result.setLuceneScore(collector.topDocs().scoreDocs[i].score);
            searchResults.add(result);
        }
        return searchResults;
    }
}
