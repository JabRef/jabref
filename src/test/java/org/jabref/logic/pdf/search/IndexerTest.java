package org.jabref.logic.pdf.search;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexerTest {


    @Test
    public void addDocumentsAndSearchInContent() throws IOException {
        Path directory = Paths.get("src/main/resources/luceneIndex");
        Indexer indexer = new Indexer(directory);


        BibDatabase database = mock(BibDatabase.class);
        BibEntry entry = mock(BibEntry.class);
        Path example = Paths.get("src/test/resources/pdfs/metaData.pdf");
        when(entry.hasField(FieldName.FILE)).thenReturn(true);
        when(entry.getField(FieldName.FILE)).thenReturn(Optional.of(example.toString()));
        when(entry.getCiteKeyOptional()).thenReturn(Optional.of("MetaData2017"));

        ObservableList<BibEntry> entries;
        entries = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(entry));
        when(database.getEntries()).thenReturn(entries);

        indexer.addDocuments(database);
    }
}
