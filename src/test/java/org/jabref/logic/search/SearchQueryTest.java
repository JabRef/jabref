package org.jabref.logic.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;

import org.jabref.logic.search.indexing.LuceneIndexer;
import org.jabref.logic.search.retrieval.LuceneSearcher;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchQueryTest {

    private LuceneSearcher searcher;
    private PreferencesService preferencesService;
    private BibDatabase bibDatabase;
    private BibDatabaseContext bibDatabaseContext;

    @BeforeEach
    public void setUp(@TempDir Path indexDir) throws IOException {
        preferencesService = mock(PreferencesService.class);
        when(preferencesService.getFilePreferences()).thenReturn(mock(FilePreferences.class));

        bibDatabase = new BibDatabase();
        bibDatabaseContext = mock(BibDatabaseContext.class);
        when(bibDatabaseContext.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
        when(bibDatabaseContext.getFulltextIndexPath()).thenReturn(indexDir);
        when(bibDatabaseContext.getDatabase()).thenReturn(bibDatabase);
        when(bibDatabaseContext.getEntries()).thenReturn(bibDatabase.getEntries());

        LuceneIndexer indexer = LuceneIndexer.of(bibDatabaseContext, preferencesService);
        indexer.createIndex();

        searcher = LuceneSearcher.of(bibDatabaseContext);
    }

    @Test
    public void nullWhenQueryBlank() {
        assertNull(new SearchQuery("", EnumSet.noneOf(SearchRules.SearchFlags.class)).getQuery());
    }

//
//    @Test
//    public void testToString() {
//        assertEquals("\"asdf\" (case sensitive, regular expression)", new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).toString());
//        assertEquals("\"asdf\" (case insensitive, plain text)", new SearchQuery("asdf", EnumSet.noneOf(SearchRules.SearchFlags.class)).toString());
//    }
//
//    @Test
//    public void testGrammarSearch() {
//        BibEntry entry = new BibEntry();
//        entry.addKeyword("one two", ',');
//        SearchQuery searchQuery = new SearchQuery("keywords=\"one two\"", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertTrue(searchQuery.isMatch(entry));
//    }
//
//    @Test
//    public void testGrammarSearchFullEntryLastCharMissing() {
//        BibEntry entry = new BibEntry();
//        entry.setField(StandardField.TITLE, "systematic revie");
//        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertFalse(searchQuery.isMatch(entry));
//    }
//
//    @Test
//    public void testGrammarSearchFullEntry() {
//        BibEntry entry = new BibEntry();
//        entry.setField(StandardField.TITLE, "systematic review");
//        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertTrue(searchQuery.isMatch(entry));
//    }
//
//    @Test
//    public void testSearchingForOpenBraketInBooktitle() {
//        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
//        e.setField(StandardField.BOOKTITLE, "Super Conference (SC)");
//
//        SearchQuery searchQuery = new SearchQuery("booktitle=\"(\"", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertTrue(searchQuery.isMatch(e));
//    }
//
//    @Test
//    public void testSearchMatchesSingleKeywordNotPart() {
//        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
//        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");
//
//        SearchQuery searchQuery = new SearchQuery("anykeyword==apple", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertFalse(searchQuery.isMatch(e));
//    }
//
//    @Test
//    public void testSearchMatchesSingleKeyword() {
//        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
//        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");
//
//        SearchQuery searchQuery = new SearchQuery("anykeyword==pineapple", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertTrue(searchQuery.isMatch(e));
//    }
//
//    @Test
//    public void testSearchAllFields() {
//        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
//        e.setField(StandardField.TITLE, "Fruity features");
//        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");
//
//        SearchQuery searchQuery = new SearchQuery("anyfield==\"fruity features\"", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertTrue(searchQuery.isMatch(e));
//    }
//
//    @Test
//    public void testSearchAllFieldsNotForSpecificField() {
//        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
//        e.setField(StandardField.TITLE, "Fruity features");
//        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");
//
//        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords!=banana", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertFalse(searchQuery.isMatch(e));
//    }
//
//    @Test
//    public void testSearchAllFieldsAndSpecificField() {
//        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
//        e.setField(StandardField.TITLE, "Fruity features");
//        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");
//
//        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords=apple", EnumSet.noneOf(SearchRules.SearchFlags.class));
//        assertTrue(searchQuery.isMatch(e));
//    }
//
//    @Test
//    public void testIsMatch() {
//        BibEntry entry = new BibEntry();
//        entry.setType(StandardEntryType.Article);
//        entry.setField(StandardField.AUTHOR, "asdf");
//
//        assertFalse(new SearchQuery("BiblatexEntryType", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
//        assertTrue(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
//        assertTrue(new SearchQuery("author=asdf", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
//    }
//
//    @Test
//    public void testIsValidQueryNotAsRegEx() {
//        assertTrue(new SearchQuery("asdf", EnumSet.noneOf(SearchRules.SearchFlags.class)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryContainsBracketNotAsRegEx() {
//        assertTrue(new SearchQuery("asdf[", EnumSet.noneOf(SearchRules.SearchFlags.class)).isValid());
//    }
//
//    @Test
//    public void testIsNotValidQueryContainsBracketNotAsRegEx() {
//        assertTrue(new SearchQuery("asdf[", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryAsRegEx() {
//        assertTrue(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryWithNumbersAsRegEx() {
//        assertTrue(new SearchQuery("123", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryContainsBracketAsRegEx() {
//        assertTrue(new SearchQuery("asdf[", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryWithEqualSignAsRegEx() {
//        assertTrue(new SearchQuery("author=asdf", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryWithNumbersAndEqualSignAsRegEx() {
//        assertTrue(new SearchQuery("author=123", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryWithEqualSignNotAsRegEx() {
//        assertTrue(new SearchQuery("author=asdf", EnumSet.noneOf(SearchRules.SearchFlags.class)).isValid());
//    }
//
//    @Test
//    public void testIsValidQueryWithNumbersAndEqualSignNotAsRegEx() {
//        assertTrue(new SearchQuery("author=123", EnumSet.noneOf(SearchRules.SearchFlags.class)).isValid());
//    }
//
//    @Test
//    public void isMatchedForNormalAndFieldBasedSearchMixed() {
//        BibEntry entry = new BibEntry();
//        entry.setType(StandardEntryType.Article);
//        entry.setField(StandardField.AUTHOR, "asdf");
//        entry.setField(StandardField.ABSTRACT, "text");
//
//        assertTrue(new SearchQuery("text AND author=asdf", EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
//    }
}
