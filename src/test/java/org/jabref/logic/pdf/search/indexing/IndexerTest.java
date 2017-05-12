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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexerTest {

    @Test
    public void addNoDocuments() throws IOException {
        // given
        Indexer indexer = new Indexer();

        BibDatabase database = mock(BibDatabase.class);
        when(database.getEntries()).thenReturn(FXCollections.emptyObservableList());

        //when
        indexer.addDocuments(database);

        //then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    public void addDocumentsAndSearchInContent() throws IOException {
        // given
        Indexer indexer = new Indexer();

        BibDatabase database = mock(BibDatabase.class);
        BibEntry entry = mock(BibEntry.class);
        when(entry.hasField(FieldName.FILE)).thenReturn(true);
        when(entry.getField(FieldName.FILE)).thenReturn(Optional.of("src/test/resources/pdfs/metaData.pdf"));
        when(entry.getCiteKeyOptional()).thenReturn(Optional.of("MetaData2017"));

        ObservableList<BibEntry> entries = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(entry));
        when(database.getEntries()).thenReturn(entries);

        //when
        indexer.addDocuments(database);

        //then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            assertEquals(6, MultiFields.getFields(reader).size());
        }
    }
}

