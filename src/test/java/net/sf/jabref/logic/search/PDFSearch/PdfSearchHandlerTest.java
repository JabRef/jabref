package net.sf.jabref.logic.search.PDFSearch;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by christoph on 02.08.16.
 */
public class PdfSearchHandlerTest {

    @Ignore
    @Test
    public void addDocumentsAndSearchInContent() {
        PdfSearchHandler handler = new PdfSearchHandler();
        //TODO use relative path instead of absolute path
        String path = "/home/christoph/workspace/jabref/src/main/resources/searchIndex/lucene";

        Document[] results = null;

        try {
            handler.initializeIndex(path);
            //TODO add test resource, this path is an absolute path on a specific development machine
//            handler.addDocumentsToServer("/home/christoph/ownCloud/uni/Semester/6. Semester/CAD/CAD");
            try {
                results = handler.searchWithIndex(path, "o*", new String[]{"content", "title"});
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertTrue(results.length > 0);

    }
}