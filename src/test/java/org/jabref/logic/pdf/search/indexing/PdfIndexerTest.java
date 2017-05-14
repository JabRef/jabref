package org.jabref.logic.pdf.search.indexing;


import java.io.IOException;
import java.util.Collections;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PdfIndexerTest {

    private PdfIndexer indexer;
    private BibDatabase database;

    @Before
    public void setUp() throws IOException {
        this.indexer = new PdfIndexer();
        this.database = new BibDatabase();
    }

    @Test
    public void createEmptyIndex() throws IOException {
        // when
        indexer.createIndex(database);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(0, reader.numDocs());
        }
    }


    @Test
    public void exampleThesisIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry("PHDThesis");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "src/test/resources/pdfs/thesis-example.pdf", "pdf")));
        database.insertEntry(entry);

        // when
        indexer.createIndex(database);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(2, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void exampleThesisIndexWithKey() throws IOException {
        // given
        BibEntry entry = new BibEntry("PHDThesis");
        entry.setCiteKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "src/test/resources/pdfs/thesis-example.pdf", "pdf")));
        database.insertEntry(entry);

        // when
        indexer.createIndex(database);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(3, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void metaDataIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry("article");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "src/test/resources/pdfs/metaData.pdf", "pdf")));

        database.insertEntry(entry);

        // when
        indexer.createIndex(database);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(5, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void testFlushIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry("PHDThesis");
        entry.setCiteKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "src/test/resources/pdfs/thesis-example.pdf", "pdf")));
        database.insertEntry(entry);

        indexer.createIndex(database);
        // index actually exists
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(3, MultiFields.getFields(reader).size());
        }

        // when
        indexer.flushIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(0, reader.numDocs());
        }
    }


    @Test
    public void exampleThesisIndexAppendMetaData() throws IOException {
        // given
        BibEntry exampleThesis = new BibEntry("PHDThesis");
        exampleThesis.setCiteKey("ExampleThesis2017");
        exampleThesis.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "src/test/resources/pdfs/thesis-example.pdf", "pdf")));
        database.insertEntry(exampleThesis);
        indexer.createIndex(database);

        // index with first entry
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(3, MultiFields.getFields(reader).size());
        }

        BibEntry metadata = new BibEntry("article");
        metadata.setCiteKey("MetaData2017");
        metadata.setFiles(Collections.singletonList(new LinkedFile("Metadata file", "src/test/resources/pdfs/metaData.pdf", "pdf")));

        // when
        indexer.addToIndex(metadata);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(2, reader.numDocs());
        }
    }
}

