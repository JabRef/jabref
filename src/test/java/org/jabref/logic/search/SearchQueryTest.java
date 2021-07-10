package org.jabref.logic.search;

import java.util.EnumSet;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchQueryTest {

    @Test
    public void testToString() {
        assertEquals("\"asdf\" (case sensitive, regular expression)", new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).toString());
        assertEquals("\"asdf\" (case insensitive, plain text)", new SearchQuery("asdf", EnumSet.noneOf(SearchFlags.class)).toString());
    }

    @Test
    public void testIsContainsBasedSearch() {
        assertTrue(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isContainsBasedSearch());
        assertTrue(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isContainsBasedSearch());
        assertFalse(new SearchQuery("author=asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isContainsBasedSearch());
    }

    @Test
    public void testIsGrammarBasedSearch() {
        assertFalse(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isGrammarBasedSearch());
        assertFalse(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isGrammarBasedSearch());
        assertTrue(new SearchQuery("author=asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isGrammarBasedSearch());
    }

    @Test
    public void testGrammarSearch() {
        BibEntry entry = new BibEntry();
        entry.addKeyword("one two", ',');
        SearchQuery searchQuery = new SearchQuery("keywords=\"one two\"", EnumSet.noneOf(SearchFlags.class));
        assertTrue(searchQuery.isMatch(entry));
    }

    @Test
    public void testGrammarSearchFullEntryLastCharMissing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "systematic revie");
        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", EnumSet.noneOf(SearchFlags.class));
        assertFalse(searchQuery.isMatch(entry));
    }

    @Test
    public void testGrammarSearchFullEntry() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "systematic review");
        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", EnumSet.noneOf(SearchFlags.class));
        assertTrue(searchQuery.isMatch(entry));
    }

    @Test
    public void testSearchingForOpenBraketInBooktitle() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.BOOKTITLE, "Super Conference (SC)");

        SearchQuery searchQuery = new SearchQuery("booktitle=\"(\"", EnumSet.noneOf(SearchFlags.class));
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchMatchesSingleKeywordNotPart() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anykeyword==apple", EnumSet.noneOf(SearchFlags.class));
        assertFalse(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchMatchesSingleKeyword() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anykeyword==pineapple", EnumSet.noneOf(SearchFlags.class));
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFields() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.TITLE, "Fruity features");
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield==\"fruity features\"", EnumSet.noneOf(SearchFlags.class));
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFieldsNotForSpecificField() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.TITLE, "Fruity features");
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords!=banana", EnumSet.noneOf(SearchFlags.class));
        assertFalse(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFieldsAndSpecificField() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.TITLE, "Fruity features");
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords=apple", EnumSet.noneOf(SearchFlags.class));
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testIsMatch() {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "asdf");

        assertFalse(new SearchQuery("BiblatexEntryType", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
        assertTrue(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
        assertTrue(new SearchQuery("author=asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
    }

    @Test
    public void testIsValidQueryNotAsRegEx() {
        assertTrue(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isValid());
    }

    @Test
    public void testIsValidQueryContainsBracketNotAsRegEx() {
        assertTrue(new SearchQuery("asdf[", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isValid());
    }

    @Test
    public void testIsNotValidQueryContainsBracketNotAsRegEx() {
        assertTrue(new SearchQuery("asdf[", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
    }

    @Test
    public void testIsValidQueryAsRegEx() {
        assertTrue(new SearchQuery("asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
    }

    @Test
    public void testIsValidQueryWithNumbersAsRegEx() {
        assertTrue(new SearchQuery("123", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
    }

    @Test
    public void testIsValidQueryContainsBracketAsRegEx() {
        assertTrue(new SearchQuery("asdf[", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
    }

    @Test
    public void testIsValidQueryWithEqualSignAsRegEx() {
        assertTrue(new SearchQuery("author=asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
    }

    @Test
    public void testIsValidQueryWithNumbersAndEqualSignAsRegEx() {
        assertTrue(new SearchQuery("author=123", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isValid());
    }

    @Test
    public void testIsValidQueryWithEqualSignNotAsRegEx() {
        assertTrue(new SearchQuery("author=asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isValid());
    }

    @Test
    public void testIsValidQueryWithNumbersAndEqualSignNotAsRegEx() {
        assertTrue(new SearchQuery("author=123", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE)).isValid());
    }

    @Test
    public void isMatchedForNormalAndFieldBasedSearchMixed() {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "asdf");
        entry.setField(StandardField.ABSTRACT, "text");

        assertTrue(new SearchQuery("text AND author=asdf", EnumSet.of(SearchRules.SearchFlags.CASE_SENSITIVE, SearchRules.SearchFlags.REGULAR_EXPRESSION)).isMatch(entry));
    }

    @Test
    public void testSimpleTerm() {
        String query = "progress";

        SearchQuery result = new SearchQuery(query, EnumSet.noneOf(SearchFlags.class));
        assertFalse(result.isGrammarBasedSearch());
    }

    @Test
    public void testGetPattern() {
        String query = "progress";
        SearchQuery result = new SearchQuery(query, EnumSet.noneOf(SearchFlags.class));
        Pattern pattern = Pattern.compile("(\\Qprogress\\E)");
        // We can't directly compare the pattern objects
        assertEquals(Optional.of(pattern.toString()), result.getPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testGetRegexpPattern() {
        String queryText = "[a-c]\\d* \\d*";
        SearchQuery regexQuery = new SearchQuery(queryText, EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));
        Pattern pattern = Pattern.compile("([a-c]\\d* \\d*)");
        assertEquals(Optional.of(pattern.toString()), regexQuery.getPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testGetRegexpJavascriptPattern() {
        String queryText = "[a-c]\\d* \\d*";
        SearchQuery regexQuery = new SearchQuery(queryText, EnumSet.of(SearchRules.SearchFlags.REGULAR_EXPRESSION));
        Pattern pattern = Pattern.compile("([a-c]\\d* \\d*)");
        assertEquals(Optional.of(pattern.toString()), regexQuery.getJavaScriptPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testEscapingInPattern() {
        // first word contain all java special regex characters
        String queryText = "<([{\\\\^-=$!|]})?*+.> word1 word2.";
        SearchQuery textQueryWithSpecialChars = new SearchQuery(queryText, EnumSet.noneOf(SearchFlags.class));
        String pattern = "(\\Q<([{\\^-=$!|]})?*+.>\\E)|(\\Qword1\\E)|(\\Qword2.\\E)";
        assertEquals(Optional.of(pattern), textQueryWithSpecialChars.getPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testEscapingInJavascriptPattern() {
        // first word contain all javascript special regex characters that should be escaped individually in text based search
        String queryText = "([{\\\\^$|]})?*+./ word1 word2.";
        SearchQuery textQueryWithSpecialChars = new SearchQuery(queryText, EnumSet.noneOf(SearchFlags.class));
        String pattern = "(\\(\\[\\{\\\\\\^\\$\\|\\]\\}\\)\\?\\*\\+\\.\\/)|(word1)|(word2\\.)";
        assertEquals(Optional.of(pattern), textQueryWithSpecialChars.getJavaScriptPatternForWords().map(Pattern::toString));
    }
}
