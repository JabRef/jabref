package org.jabref.logic.search;

import org.jabref.model.pdf.search.EnglishStemAnalyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

public class LuceneTest {
    public static void main(String[] args) throws Exception {
        // Setup the analyzer
        Analyzer analyzer = new EnglishStemAnalyzer();

        // Store the index in memory
        Directory directory = new ByteBuffersDirectory();

        // Configure index writer
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        // Index sample data
        String[] texts = {"running", "runner", "ran", "trial", "trials"};
        for (String text : texts) {
            Document document = new Document();
            document.add(new TextField("content", text, Field.Store.YES));
            indexWriter.addDocument(document);
        }
        indexWriter.close();

        search("trials", directory, analyzer);
    }

    public static void search(String queryString, Directory directory, Analyzer analyzer) throws Exception {
        Query query = new QueryParser("content", analyzer).parse(queryString);
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
        ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;

        for (ScoreDoc scoreDoc : hits) {
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println(doc.get("content"));
        }
    }
}
