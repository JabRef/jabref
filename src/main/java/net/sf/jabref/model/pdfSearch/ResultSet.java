package net.sf.jabref.model.pdfSearch;

import java.util.List;

import net.sf.jabref.logic.search.PDFSearch.SearchFieldConstants;

import org.apache.lucene.document.Document;

/**
 * Created by christoph on 04.08.16.
 */
public class ResultSet {

    private List<SearchResult> searchResults;

    public ResultSet(Document[] documents) {
        this.searchResults = mapToSearchResults(documents);
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

        for (Document doc : documents) {
            SearchResult result = new SearchResult();
            for (String field : SearchFieldConstants.PDF_FIELDS) {
                result.mapField(field, doc.getField(field).stringValue());
            }
            searchResults.add(result);
        }
        return searchResults;
    }
}
