package org.jabref.model.pdf.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;

public class ResultSet {

    public final List<SearchResult> searchResults;

    public ResultSet(Document[] documents) {
        this.searchResults = Collections.unmodifiableList(mapToSearchResults(documents));
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
     */
    private List<SearchResult> mapToSearchResults(Document[] documents) {
        List<SearchResult> results = new ArrayList<>(documents.length);

        for (int i = 0; i < documents.length; i++) {
            SearchResult result = new SearchResult();
            for (String field : SearchFieldConstants.PDF_FIELDS) {
                result.mapField(field, documents[i].getField(field).stringValue());
            }
            result.setLuceneScore(0);// TODO: use an actual scoring function
            results.add(result);
        }
        return results;
    }
}
