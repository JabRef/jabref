package org.jabref.logic.search;

public class DatabaseSearcherWithBibFilesTest {

//    private static BibEntry titleSentenceCased = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("title-sentence-cased")
//            .withField(StandardField.TITLE, "Title Sentence Cased");
//    private static BibEntry titleMixedCased = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("title-mixed-cased")
//            .withField(StandardField.TITLE, "TiTle MiXed CaSed");
//    private static BibEntry titleUpperCased = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("title-upper-cased")
//            .withField(StandardField.TITLE, "TITLE UPPER CASED");
//
//    private static BibEntry mininimalSentenceCase = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("minimal-sentence-case")
//            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-sentence-case.pdf", StandardFileType.PDF.getName())));
//    private static BibEntry minimalAllUpperCase = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("minimal-all-upper-case")
//            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-all-upper-case.pdf", StandardFileType.PDF.getName())));
//    private static BibEntry minimalMixedCase = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("minimal-mixed-case")
//            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-mixed-case.pdf", StandardFileType.PDF.getName())));
//    private static BibEntry minimalNoteSentenceCase = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("minimal-note-sentence-case")
//            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-sentence-case.pdf", StandardFileType.PDF.getName())));
//    private static BibEntry minimalNoteAllUpperCase = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("minimal-note-all-upper-case")
//            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-all-upper-case.pdf", StandardFileType.PDF.getName())));
//    private static BibEntry minimalNoteMixedCase = new BibEntry(StandardEntryType.Misc)
//            .withCitationKey("minimal-note-mixed-case")
//            .withFiles(Collections.singletonList(new LinkedFile("", "minimal-note-mixed-case.pdf", StandardFileType.PDF.getName())));
//
//    FilePreferences filePreferences = mock(FilePreferences.class);
//
//    @TempDir
//    private Path indexDir;
//    private PdfIndexer pdfIndexer;
//
//    private BibDatabase initializeDatabaseFromPath(String testFile) throws Exception {
//        return initializeDatabaseFromPath(Path.of(Objects.requireNonNull(DatabaseSearcherWithBibFilesTest.class.getResource(testFile)).toURI()));
//    }
//
//    private BibDatabase initializeDatabaseFromPath(Path testFile) throws Exception {
//        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).importDatabase(testFile);
//        BibDatabase database = result.getDatabase();
//
//        BibDatabaseContext context = mock(BibDatabaseContext.class);
//        when(context.getFileDirectories(Mockito.any())).thenReturn(List.of(testFile.getParent()));
//        when(context.getFulltextIndexPath()).thenReturn(indexDir);
//        when(context.getDatabase()).thenReturn(database);
//        when(context.getEntries()).thenReturn(database.getEntries());
//
////         Required because of {@Link org.jabref.model.search.FullTextSearchRule.FullTextSearchRule}
//        Globals.stateManager.setActiveDatabase(context);
//        PreferencesService preferencesService = mock(PreferencesService.class);
//        when(preferencesService.getFilePreferences()).thenReturn(filePreferences);
//        Globals.prefs = preferencesService;
//
//        pdfIndexer = PdfIndexerManager.getIndexer(context, filePreferences);
//        // Alternative - For debugging with Luke (part of the Apache Lucene distribution)
//        // pdfIndexer = PdfIndexer.of(context, Path.of("C:\\temp\\index"), filePreferences);
//
//        pdfIndexer.rebuildIndex();
//        return database;
//    }
//
//    @AfterEach
//    public void tearDown() throws Exception {
//        pdfIndexer.close();
//    }
//
//    private static Stream<Arguments> searchLibrary() {
//        return Stream.of(
//                // empty library
//                Arguments.of(List.of(), "empty.bib", "Test", EnumSet.noneOf(SearchFlags.class)),
//
//                // test-library-title-casing
//
//                Arguments.of(List.of(), "test-library-title-casing.bib", "NotExisting", EnumSet.noneOf(SearchFlags.class)),
//                Arguments.of(List.of(titleSentenceCased, titleMixedCased, titleUpperCased), "test-library-title-casing.bib", "Title", EnumSet.noneOf(SearchFlags.SearchFlags.class)),
//
//                Arguments.of(List.of(), "test-library-title-casing.bib", "title=NotExisting", EnumSet.noneOf(SearchFlags.class)),
//                Arguments.of(List.of(titleSentenceCased, titleMixedCased, titleUpperCased), "test-library-title-casing.bib", "title=Title", EnumSet.noneOf(SearchFlags.SearchFlags.class)),
//
//                Arguments.of(List.of(), "test-library-title-casing.bib", "title=TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
//                Arguments.of(List.of(titleSentenceCased), "test-library-title-casing.bib", "title=Title", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
//
//                Arguments.of(List.of(), "test-library-title-casing.bib", "TiTLE", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
//                Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
//
//                Arguments.of(List.of(), "test-library-title-casing.bib", "title=NotExisting", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
//                Arguments.of(List.of(titleMixedCased), "test-library-title-casing.bib", "title=TiTle", EnumSet.of(SearchFlags.CASE_SENSITIVE)),
//
//                Arguments.of(List.of(), "test-library-title-casing.bib", "[Y]", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
//                Arguments.of(List.of(titleUpperCased), "test-library-title-casing.bib", "[U]", EnumSet.of(SearchFlags.REGULAR_EXPRESSION)),
//
//                // Word boundaries
//                Arguments.of(List.of(), "test-library-title-casing.bib", "\\bTit\\b", EnumSet.of(SearchFlags.REGULAR_EXPRESSION, SearchFlags.CASE_SENSITIVE)),
//                Arguments.of(List.of(titleSentenceCased), "test-library-title-casing.bib", "\\bTitle\\b", EnumSet.of(SearchFlags.REGULAR_EXPRESSION, SearchFlags.CASE_SENSITIVE)),
//
//                // test-library-with-attached-files
//
//                Arguments.of(List.of(), "test-library-with-attached-files.bib", "This is a test.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
//
//                Arguments.of(List.of(mininimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "This is a short sentence, comma included.", EnumSet.of(SearchFlags.FULLTEXT)),
//                Arguments.of(List.of(mininimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchFlags.FULLTEXT)),
//                // TODO: PDF search does not support case sensitive search (yet)
//                 Arguments.of(List.of(minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "THIS", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
//                 Arguments.of(List.of(minimalAllUpperCase), "test-library-with-attached-files.bib", "THIS is a short sentence, comma included.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
//                 Arguments.of(List.of(minimalSentenceCase, minimalAllUpperCase, minimalMixedCase), "test-library-with-attached-files.bib", "comma", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
//                 Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "THIS IS A SHORT SENTENCE, COMMA INCLUDED.", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
//
//                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchFlags.FULLTEXT)),
//
//                Arguments.of(List.of(minimalNoteSentenceCase, minimalNoteAllUpperCase, minimalNoteMixedCase), "test-library-with-attached-files.bib", "world", EnumSet.of(SearchFlags.FULLTEXT)),
//                Arguments.of(List.of(minimalNoteSentenceCase, minimalNoteAllUpperCase, minimalNoteMixedCase), "test-library-with-attached-files.bib", "Hello World", EnumSet.of(SearchFlags.FULLTEXT)),
//                // TODO: PDF search does not support case sensitive search (yet)
//                 Arguments.of(List.of(minimalNoteAllUpperCase), "test-library-with-attached-files.bib", "HELLO WORLD", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE)),
//                Arguments.of(List.of(), "test-library-with-attached-files.bib", "NotExisting", EnumSet.of(SearchFlags.FULLTEXT, SearchFlags.CASE_SENSITIVE))
//        );
//    }
//
//    @ParameterizedTest(name = "{index} => query={2}, searchFlags={3}, testFile={1}, expected={0}")
//    @MethodSource
//    public void searchLibrary(List<BibEntry> expected, String testFile, String query, EnumSet<SearchFlags> searchFlags) throws Exception {
//        BibDatabase database = initializeDatabaseFromPath(testFile);
//        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery(query, searchFlags), database).getMatches();
//        assertEquals(expected, matches);
//    }
}
