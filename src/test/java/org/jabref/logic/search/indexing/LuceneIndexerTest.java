package org.jabref.logic.search.indexing;

public class LuceneIndexerTest {
    /*
    private LuceneIndexer indexer;
    private BibDatabase database;
    private BibDatabaseContext context = mock(BibDatabaseContext.class);

    @BeforeEach
    void setUp(@TempDir Path indexDir) throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(true);
        PreferencesService preferencesService = mock(PreferencesService.class);
        when(preferencesService.getFilePreferences()).thenReturn(filePreferences);
        this.database = new BibDatabase();

        this.context = mock(BibDatabaseContext.class);
        when(context.getDatabasePath()).thenReturn(Optional.of(Path.of("src/test/resources/pdfs/")));
        when(context.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(context.getDatabase()).thenReturn(database);
        when(context.getEntries()).thenReturn(database.getEntries());
        this.indexer = LuceneIndexer.of(context, preferencesService);
    }

    @Test
    void exampleThesisIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }
    }

    @Test
    void dontIndexNonPdf() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.AUX.getName())));
        database.insertEntry(entry);

        // when
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(1, reader.numDocs());
        }
    }

    @Test
    void dontIndexOnlineLinks() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "https://raw.githubusercontent.com/JabRef/jabref/main/src/test/resources/pdfs/thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(1, reader.numDocs());
        }
    }

    @Test
    void exampleThesisIndexWithKey() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }
    }

    @Test
    void metaDataIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "metaData.pdf", StandardFileType.PDF.getName())));

        database.insertEntry(entry);

        // when
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(2, reader.numDocs());
        }
    }

    @Test
    public void flushIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }
        // index actually exists
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }

        // when
        indexer.flushIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    void exampleThesisIndexAppendMetaData() throws IOException {
        // given
        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis);
        exampleThesis.setCitationKey("ExampleThesis2017");
        exampleThesis.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(exampleThesis);
        indexer.createIndex();
        for (BibEntry bibEntry : context.getEntries()) {
            indexer.addBibFieldsToIndex(bibEntry);
            indexer.addLinkedFilesToIndex(bibEntry);
        }

        // index with first entry
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }

        BibEntry metadata = new BibEntry(StandardEntryType.Article);
        metadata.setCitationKey("MetaData2017");
        metadata.setFiles(Collections.singletonList(new LinkedFile("Metadata file", "metaData.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addBibFieldsToIndex(metadata);
        indexer.addLinkedFilesToIndex(metadata);

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(36, reader.numDocs());
        }
    }
    */
}
