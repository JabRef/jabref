package org.jabref.logic.pdf.search;


import java.io.IOException;


import org.jabref.model.database.BibDatabase;
import org.jabref.model.pdf.search.ResultSet;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class IndexerTest {


    @Test
    public void addDocumentsAndSearchInContent() {
        Indexer handler = new Indexer();
        //TODO use relative path instead of absolute path
        String path = "/home/christoph/workspace/jabref/src/main/resources/searchIndex/lucene";


        try {
            handler.initializeIndex(path);
            //TODO add test resource, this path is an absolute path on a specific development machine

            handler.addDocuments(new BibDatabase());
            try {
                ResultSet results = handler.searchWithIndex(path, "o*", new String[]{"content", "title"});
                Assert.assertTrue(results.searchResults.size() > 0);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
