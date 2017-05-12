package org.jabref.logic.pdf.search.indexing;


import java.io.IOException;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexerTest {

    private Indexer indexer;
    private BibDatabase database;

    @Before
    public void setUp() throws IOException {
        this.indexer = new Indexer();
        this.database = mock(BibDatabase.class);
    }

    @Test
    public void addNoDocuments() throws IOException {
        // given
        when(database.getEntries()).thenReturn(FXCollections.emptyObservableList());

        // when
        indexer.createIndex(database);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    public void metaDataOneEntry() throws IOException {
        // given
        BibEntry entry = mock(BibEntry.class);
        when(entry.hasField(FieldName.FILE)).thenReturn(true);
        when(entry.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/metaData.pdf"));
        when(entry.getCiteKeyOptional()).thenReturn(Optional.of("MetaData2017"));

        ObservableList<BibEntry> entries = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(entry));
        when(database.getEntries()).thenReturn(entries);

        // when
        indexer.createIndex(database);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(6, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void examplePdf() throws IOException {
        // given
        BibEntry entry = mock(BibEntry.class);
        when(entry.hasField(FieldName.FILE)).thenReturn(true);
        when(entry.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/example.pdf"));
        when(entry.getCiteKeyOptional()).thenReturn(Optional.of("Example2017"));

        ObservableList<BibEntry> entries = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(entry));
        when(database.getEntries()).thenReturn(entries);

        // when
        indexer.createIndex(database);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(3, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void examplePdfAndMetaData() throws IOException {
        // given
        BibEntry examplePdf = mock(BibEntry.class);
        when(examplePdf.hasField(FieldName.FILE)).thenReturn(true);
        when(examplePdf.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/example.pdf"));
        when(examplePdf.getCiteKeyOptional()).thenReturn(Optional.of("Example2017"));

        ObservableList<BibEntry> entries = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(examplePdf));
        when(database.getEntries()).thenReturn(entries);

        indexer.createIndex(database);

        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(3, MultiFields.getFields(reader).size());
        }


        BibEntry metaDataEntry = mock(BibEntry.class);
        when(metaDataEntry.hasField(FieldName.FILE)).thenReturn(true);
        when(metaDataEntry.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/metaData.pdf"));
        when(metaDataEntry.getCiteKeyOptional()).thenReturn(Optional.of("MetaData2017"));

        // when
        indexer.appendToIndex(metaDataEntry);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(2, reader.numDocs());
        }
    }

    @Test
    public void flushIndex() throws IOException {
        // given
        BibEntry entry = mock(BibEntry.class);
        when(entry.hasField(FieldName.FILE)).thenReturn(true);
        when(entry.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/metaData.pdf"));
        when(entry.getCiteKeyOptional()).thenReturn(Optional.of("MetaData2017"));

        ObservableList<BibEntry> entries = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(entry));
        when(database.getEntries()).thenReturn(entries);

        indexer.createIndex(database);

        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(6, MultiFields.getFields(reader).size());
        }

        // when
        indexer.flushIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(0, reader.numDocs());
        }
    }
}

