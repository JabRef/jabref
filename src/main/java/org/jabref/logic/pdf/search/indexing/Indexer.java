package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Paths;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

public class Indexer {
    private static final Log LOGGER = LogFactory.getLog(Indexer.class);

    private final Directory directoryToIndex;

    public Indexer() throws IOException {
        this.directoryToIndex = new SimpleFSDirectory(Paths.get("src/main/resources/luceneIndex"));
    }

    public Directory getIndexDirectory() {
        return this.directoryToIndex;
    }

    /**
     * Adds all pdf files linked to an entry in the database to the lucene search index
     *
     * @param database a bibtex database to link the pdf files to
     */
    public void addDocuments(BibDatabase database) {

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter indexWriter = new IndexWriter(directoryToIndex, indexWriterConfig)) {

            database.getEntries().stream().
                    filter(entry -> entry.hasField(FieldName.FILE)).
                    filter(entry -> entry.getCiteKeyOptional().isPresent()).
                    forEach(entry -> addToIndex(entry, indexWriter));
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    private void addToIndex(BibEntry entry, IndexWriter indexWriter) {
        try {
            indexWriter.addDocument(new DocumentReader(entry).readPDFContents());
        } catch (IOException e) {
            LOGGER.debug("Document could not be added to the index.", e);
        }
    }

}
