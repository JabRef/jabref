package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PdfIndexerTest {

    private PdfIndexer indexer;
    private BibDatabase database;
    private BibDatabaseContext context = mock(BibDatabaseContext.class);

    @BeforeEach
    public void setUp() throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);
        this.indexer = new PdfIndexer(filePreferences);
        this.database = new BibDatabase();
        when(context.getDatabasePath()).thenReturn(Optional.of(Path.of("src/test/resources/pdfs/")));

        this.context = mock(BibDatabaseContext.class);
        when(context.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
    }

    @Test
    public void createEmptyIndex() throws IOException {
        // when
        indexer.createIndex(database, context);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    public void exampleThesisIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", "pdf")));
        database.insertEntry(entry);

        // when
        indexer.createIndex(database, context);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            // assertEquals(1, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void exampleThesisIndexWithKey() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", "pdf")));
        database.insertEntry(entry);

        // when
        indexer.createIndex(database, context);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            // assertEquals(2, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void metaDataIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "metaData.pdf", "pdf")));

        database.insertEntry(entry);

        // when
        indexer.createIndex(database, context);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            // assertEquals(5, MultiFields.getFields(reader).size());
        }
    }

    @Test
    public void testFlushIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", "pdf")));
        database.insertEntry(entry);

        indexer.createIndex(database, context);
        // index actually exists
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            // assertEquals(2, MultiFields.getFields(reader).size());
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
        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis);
        exampleThesis.setCitationKey("ExampleThesis2017");
        exampleThesis.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", "pdf")));
        database.insertEntry(exampleThesis);
        indexer.createIndex(database, context);

        // index with first entry
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(1, reader.numDocs());
            // assertEquals(2, MultiFields.getFields(reader).size());
        }

        BibEntry metadata = new BibEntry(StandardEntryType.Article);
        metadata.setCitationKey("MetaData2017");
        metadata.setFiles(Collections.singletonList(new LinkedFile("Metadata file", "metaData.pdf", "pdf")));

        // when
        indexer.addToIndex(metadata, null);

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.getIndexDirectory())) {
            assertEquals(2, reader.numDocs());
        }
    }
}

