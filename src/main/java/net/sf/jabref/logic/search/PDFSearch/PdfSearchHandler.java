package net.sf.jabref.logic.search.PDFSearch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Created by christoph on 02.08.16.
 */
public class PdfSearchHandler {

    Directory directoryToIndex;
    StandardAnalyzer analyzer;
    //To be deleted, only testing purpose
    List<String> l = new ArrayList();

    /**
     * Initialize an index to search for pdfs in a specific directory
     *
     * @param pathToIndex path to the directory where the index is created
     */
    public void initializeIndex(String pathToIndex) throws IOException {

        l.add("you all");
        l.add("visit");
        l.add("some blog");
        l.add("sometimes");

        directoryToIndex = FSDirectory.getDirectory(pathToIndex);
        analyzer = new StandardAnalyzer();

    }

    public void addDocumentToServer(String pathToDocumentFolder) throws IOException {

        IndexWriter indexWriter = new IndexWriter(directoryToIndex, analyzer, true,
                IndexWriter.MaxFieldLength.UNLIMITED);

        File[] files;
        if (new File(pathToDocumentFolder).isDirectory()) {
            files = new File(pathToDocumentFolder).listFiles();
        } else {
            files = new File[]{new File(pathToDocumentFolder)};
        }

        for (int i = 0; i < files.length; i++) {

            if (files[i].getName().endsWith("pdf")) {
                indexWriter.addDocument(PdfContentReader.readContentFromPDFToString(files[i]));
            }
        }
        indexWriter.close();
    }

    public Document[] searchWithIndex(String pathToDirectory, String searchQuery, String[] fields) throws ParseException, IOException {

        Query q = new MultiFieldQueryParser(fields,
                analyzer).parse(searchQuery);

        int hitsPerPage = 10;
        IndexSearcher searcher = new IndexSearcher(pathToDirectory);
        TopDocCollector collector = new TopDocCollector(hitsPerPage);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        Document[] searchResults = new Document[hits.length];

        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            searchResults[i] = searcher.doc(docId);
        }
        return searchResults;
    }
}
