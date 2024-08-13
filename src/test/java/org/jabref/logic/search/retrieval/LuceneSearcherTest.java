package org.jabref.logic.search.retrieval;

public class LuceneSearcherTest {

//    private LuceneSearcher searcher;
//    private PreferencesService preferencesService;
//    private BibDatabase bibDatabase;
//    private BibDatabaseContext bibDatabaseContext;
//
//    @BeforeEach
//    public void setUp(@TempDir Path indexDir) throws IOException {
//        preferencesService = mock(PreferencesService.class);
//        when(preferencesService.getFilePreferences()).thenReturn(mock(FilePreferences.class));
//
//        bibDatabase = new BibDatabase();
//        bibDatabaseContext = mock(BibDatabaseContext.class);
//        when(bibDatabaseContext.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
//        when(bibDatabaseContext.getFulltextIndexPath()).thenReturn(indexDir);
//        when(bibDatabaseContext.getDatabase()).thenReturn(bibDatabase);
//        when(bibDatabaseContext.getEntries()).thenReturn(bibDatabase.getEntries());
//    }
//
//    private void initIndexer() throws IOException {
//        LuceneIndexer indexer = LuceneIndexer.of(bibDatabaseContext, preferencesService);
//        searcher = LuceneSearcher.of(bibDatabaseContext);
//
//        for (BibEntry bibEntry : bibDatabaseContext.getEntries()) {
//            indexer.addBibFieldsToIndex(bibEntry);
//            indexer.addLinkedFilesToIndex(bibEntry);
//        }
//    }
//
//    @Test
//    public void searchForTest() throws IOException {
//        insertPdfsForSearch();
//        initIndexer();
//
//        HashMap<BibEntry, LuceneSearchResults> searchResults = searcher.search(new SearchQuery("", EnumSet.noneOf(SearchFlags.class)));
//        int hits = searchResults.keySet().stream().mapToInt(key -> searchResults.get(key).numSearchResults()).sum();
//        assertEquals(8, hits);
//    }
//
//    @Test
//    public void searchForUniversity() throws IOException {
//        insertPdfsForSearch();
//        initIndexer();
//
//        HashMap<BibEntry, LuceneSearchResults> searchResults = searcher.search(new SearchQuery("University", EnumSet.noneOf(SearchFlags.class)));
//        int hits = searchResults.keySet().stream().mapToInt(key -> searchResults.get(key).numSearchResults()).sum();
//        assertEquals(1, hits);
//    }
//
//    @Test
//    public void searchForStopWord() throws IOException {
//        insertPdfsForSearch();
//        initIndexer();
//
//        HashMap<BibEntry, LuceneSearchResults> searchResults = searcher.search(new SearchQuery("and", EnumSet.noneOf(SearchFlags.class)));
//        int hits = searchResults.keySet().stream().mapToInt(key -> searchResults.get(key).numSearchResults()).sum();
//        assertEquals(0, hits);
//    }
//
//    @Test
//    public void searchForSecond() throws IOException {
//        insertPdfsForSearch();
//        initIndexer();
//
//        HashMap<BibEntry, LuceneSearchResults> searchResults = searcher.search(new SearchQuery("second", EnumSet.noneOf(SearchFlags.class)));
//        int hits = searchResults.keySet().stream().mapToInt(key -> searchResults.get(key).numSearchResults()).sum();
//        assertEquals(4, hits);
//    }
//
//    @Test
//    public void searchForAnnotation() throws IOException {
//        insertPdfsForSearch();
//        initIndexer();
//
//        HashMap<BibEntry, LuceneSearchResults> searchResults = searcher.search(new SearchQuery("annotation", EnumSet.noneOf(SearchFlags.class)));
//        int hits = searchResults.keySet().stream().mapToInt(key -> searchResults.get(key).numSearchResults()).sum();
//        assertEquals(2, hits);
//    }
//
//    @Test
//    public void searchForEmptyString() throws IOException {
//        insertPdfsForSearch();
//        initIndexer();
//
//        HashMap<BibEntry, LuceneSearchResults> searchResults = searcher.search(new SearchQuery("", EnumSet.noneOf(SearchFlags.class)));
//        int hits = searchResults.keySet().stream().mapToInt(key -> searchResults.get(key).numSearchResults()).sum();
//        assertEquals(0, hits);
//    }
//
//    @Test
//    public void searchWithNullString() {
//        assertThrows(NullPointerException.class, () -> searcher.search(null));
//    }
//
//    private void insertPdfsForSearch() {
//        when(preferencesService.getFilePreferences().shouldFulltextIndexLinkedFiles()).thenReturn(true);
//
//        BibEntry examplePdf = new BibEntry(StandardEntryType.Article);
//        examplePdf.setFiles(Collections.singletonList(new LinkedFile("Example Entry", "example.pdf", StandardFileType.PDF.getName())));
//        bibDatabase.insertEntry(examplePdf);
//
//        BibEntry metaDataEntry = new BibEntry(StandardEntryType.Article);
//        metaDataEntry.setFiles(Collections.singletonList(new LinkedFile("Metadata Entry", "metaData.pdf", StandardFileType.PDF.getName())));
//        metaDataEntry.setCitationKey("MetaData2017");
//        bibDatabase.insertEntry(metaDataEntry);
//
//        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis);
//        exampleThesis.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
//        exampleThesis.setCitationKey("ExampleThesis");
//        bibDatabase.insertEntry(exampleThesis);
//    }
}
