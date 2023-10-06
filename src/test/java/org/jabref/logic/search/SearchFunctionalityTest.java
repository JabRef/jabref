package org.jabref.logic.search;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.pdf.search.indexing.PdfIndexer;
import org.jabref.logic.pdf.search.retrieval.PdfSearcher;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchFunctionalityTest {

    private BibDatabase database;
    private PdfSearcher search;
    private BibDatabaseContext context;
    private PdfIndexer indexer;

    BibEntry entry1A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry1")
            .withField(StandardField.AUTHOR, "Test")
            .withField(StandardField.TITLE, "cASe");
    BibEntry entry2A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry2")
            .withField(StandardField.AUTHOR, "test")
            .withField(StandardField.TITLE, "casE");
    BibEntry entry3A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry3")
            .withField(StandardField.AUTHOR, "tESt")
            .withField(StandardField.TITLE, "Case");
    BibEntry entry4A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry4")
            .withField(StandardField.AUTHOR, "tesT")
            .withField(StandardField.TITLE, "CASE");
    BibEntry entry5A = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry5")
            .withField(StandardField.AUTHOR, "TEST")
            .withField(StandardField.TITLE, "case");

    BibEntry entry1B = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry1")
            .withField(StandardField.AUTHOR, "Test")
            .withField(StandardField.TITLE, "Case");
    BibEntry entry4B = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry4")
            .withField(StandardField.AUTHOR, "Special")
            .withField(StandardField.TITLE, "192? title.");

    BibEntry minimal1 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal1");
    BibEntry minimal2 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal2");
    BibEntry minimal3 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal3");
    BibEntry minimalNote1 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note1");
    BibEntry minimalNote2 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note2");
    BibEntry minimalNote3 = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("minimal-note3");

    BibEntry entry1D = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry1")
            .withField(StandardField.AUTHOR, "Test")
            .withField(StandardField.TITLE, "Case")
            .withField(StandardField.GROUPS, "A");
    BibEntry entry2D = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("entry2")
            .withField(StandardField.AUTHOR, "TEST")
            .withField(StandardField.TITLE, "CASE")
            .withField(StandardField.GROUPS, "A");

    @BeforeEach
    public void setUp(@TempDir Path indexDir) throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);
        database = new BibDatabase();
        context = mock(BibDatabaseContext.class);

        minimal1.setFiles(Collections.singletonList(new LinkedFile("Minimal", "minimal.pdf", StandardFileType.PDF.getName())));
        minimal2.setFiles(Collections.singletonList(new LinkedFile("Minimal 1", "minimal1.pdf", StandardFileType.PDF.getName())));
        minimal3.setFiles(Collections.singletonList(new LinkedFile("Minimal 2", "minimal2.pdf", StandardFileType.PDF.getName())));
        minimalNote1.setFiles(Collections.singletonList(new LinkedFile("Minimalnote", "minimal-note.pdf", StandardFileType.PDF.getName())));
        minimalNote2.setFiles(Collections.singletonList(new LinkedFile("Minimalnote 1", "minimal-note1.pdf", StandardFileType.PDF.getName())));
        minimalNote3.setFiles(Collections.singletonList(new LinkedFile("Minimalnote 2", "minimal-note2.pdf", StandardFileType.PDF.getName())));

        when(context.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/org/jabref/logic/search")));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(context.getDatabase()).thenReturn(database);
        when(context.getEntries()).thenReturn(database.getEntries());

        indexer = PdfIndexer.of(context, filePreferences);
        search = PdfSearcher.of(context);

        indexer.createIndex();
        indexer.addToIndex(context);
    }

    private void initializeDatabaseFromPath(Path testFile) throws IOException {
        ParserResult result = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).importDatabase(testFile);
        context = new BibDatabaseContext(result.getDatabase(), result.getMetaData());

        database = context.getDatabase();
        search = PdfSearcher.of(context);

        indexer.addToIndex(context);
    }

    @Test
    public void testEmptyLibrarySearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("empty.bib")).toURI()));

        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("Test", EnumSet.noneOf(SearchRules.SearchFlags.class)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testUpperAndLowerWordSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-A.bib")).toURI()));

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("Test", EnumSet.noneOf(SearchRules.SearchFlags.class)), database).getMatches();
        assertEquals(List.of(entry1A, entry2A, entry3A, entry4A, entry5A), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("Best", EnumSet.noneOf(SearchRules.SearchFlags.class)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testSimpleSingleFieldSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-A.bib")).toURI()));

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("author=Test", EnumSet.noneOf(SearchRules.SearchFlags.class)), database).getMatches();
        assertEquals(List.of(entry1A, entry2A, entry3A, entry4A, entry5A), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("author=Case", EnumSet.noneOf(SearchRules.SearchFlags.class)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testSimpleMultipleFieldSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-A.bib")).toURI()));

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("author=Test and title=Case", EnumSet.noneOf(SearchRules.SearchFlags.class)), database).getMatches();
        assertEquals(List.of(entry1A, entry2A, entry3A, entry4A, entry5A), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("author=Test and title=Test", EnumSet.noneOf(SearchRules.SearchFlags.class)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testSensitiveWordSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-A.bib")).toURI()));

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("Test", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(List.of(entry1A), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("TesT", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testSensitiveMultipleFieldSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-A.bib")).toURI()));

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("author=Test and title=cASe", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(List.of(entry1A), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("author=Test and title=case", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testSimpleRegularExpression() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-B.bib")).toURI()));

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("[/9]", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)), database).getMatches();
        assertEquals(List.of(entry4B), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("[/8]", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testSensitiveRegularExpression() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-B.bib")).toURI()));

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("\\bCase\\b", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION, SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(List.of(entry1B), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("\\bCas\\b", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION, SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

/*
    @Test
    public void testSimplePDFFulltextSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-C.bib")).toURI()));

        //@Test uses PDFReader

        //Positive search test
        PdfSearchResults resultsPositive = search.search("This is a short sentence, comma included.", 10);
        assertEquals(3, resultsPositive.numSearchResults());
        //Negative search test
        PdfSearchResults resultsNegative = search.search("This is a test.", 10);
        assertEquals(0, resultsNegative.numSearchResults());
    }

    @Test
    public void testSimplePDFNoteFulltextSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-C.bib")).toURI()));

        //@Test uses PDFReader

        //Positive search test
        PdfSearchResults resultsPositive = search.search("Hello World", 10);
        assertEquals(3, resultsPositive.numSearchResults());
        //Negative search test
        PdfSearchResults resultsNegative = search.search("User Test", 10);
        assertEquals(0, resultsNegative.numSearchResults());
    }

    @Test
    public void testSensitivePDFFulltextSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-C.bib")).toURI()));

        //@Test uses DatabaseSearcher

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("This is a short sentence, comma included.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(List.of(minimal1, minimalNote1), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("This is a test.", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }

    @Test
    public void testSensitivePDFNoteFulltextSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-C.bib")).toURI()));

        //@Test uses DatabaseSearcher

        //Positive search test
        List<BibEntry> matches = new DatabaseSearcher(new SearchQuery("Hello World", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(List.of(minimalNote1), matches);
        //Negative search test
        matches = new DatabaseSearcher(new SearchQuery("User Test", EnumSet.of(SearchRules.SearchFlags.FULLTEXT, SearchRules.SearchFlags.CASE_SENSITIVE)), database).getMatches();
        assertEquals(Collections.emptyList(), matches);
    }
*/
    @Test
    public void testSimpleGroupSearch() throws IOException, URISyntaxException {
        initializeDatabaseFromPath(Path.of(Objects.requireNonNull(SearchFunctionalityTest.class.getResource("test-library-D.bib")).toURI()));
        SearchGroup groupPositive = new SearchGroup("A", GroupHierarchyType.INDEPENDENT, "Test", EnumSet.noneOf(SearchRules.SearchFlags.class));
        SearchGroup groupNegative = new SearchGroup("A", GroupHierarchyType.INDEPENDENT, "Unknown", EnumSet.noneOf(SearchRules.SearchFlags.class));

        List<BibEntry> positiveResult = new ArrayList<>();

        positiveResult.add(entry1D);
        positiveResult.add(entry2D);

        //Positive search test
        assertTrue(groupPositive.containsAll(positiveResult));
        //Negative search test
        assertFalse(groupNegative.containsAll(positiveResult));
    }
}


