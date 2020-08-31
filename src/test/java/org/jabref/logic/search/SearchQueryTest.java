package org.jabref.logic.search;

import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchQueryTest {

    @Test
    public void testToString() {
        assertEquals("\"asdf\" (case sensitive, regular expression)", new SearchQuery("asdf", true, true).toString());
        assertEquals("\"asdf\" (case insensitive, plain text)", new SearchQuery("asdf", false, false).toString());
    }

    @Test
    public void testIsContainsBasedSearch() {
        assertTrue(new SearchQuery("asdf", true, false).isContainsBasedSearch());
        assertTrue(new SearchQuery("asdf", true, true).isContainsBasedSearch());
        assertFalse(new SearchQuery("author=asdf", true, false).isContainsBasedSearch());
    }

    @Test
    public void testIsGrammarBasedSearch() {
        assertFalse(new SearchQuery("asdf", true, false).isGrammarBasedSearch());
        assertFalse(new SearchQuery("asdf", true, true).isGrammarBasedSearch());
        assertTrue(new SearchQuery("author=asdf", true, false).isGrammarBasedSearch());
    }

    @Test
    public void testGrammarSearch() {
        BibEntry entry = new BibEntry();
        entry.addKeyword("one two", ',');
        SearchQuery searchQuery = new SearchQuery("keywords=\"one two\"", false, false);
        assertTrue(searchQuery.isMatch(entry));
    }

    @Test
    public void testGrammarSearchFullEntryLastCharMissing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "systematic revie");
        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", false, false);
        assertFalse(searchQuery.isMatch(entry));
    }

    @Test
    public void testGrammarSearchFullEntry() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "systematic review");
        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", false, false);
        assertTrue(searchQuery.isMatch(entry));
    }

    @Test
    public void testSearchingForOpenBraketInBooktitle() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.BOOKTITLE, "Super Conference (SC)");

        SearchQuery searchQuery = new SearchQuery("booktitle=\"(\"", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchMatchesSingleKeywordNotPart() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anykeyword==apple", false, false);
        assertFalse(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchMatchesSingleKeyword() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anykeyword==pineapple", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFields() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.TITLE, "Fruity features");
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield==\"fruity features\"", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFieldsNotForSpecificField() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.TITLE, "Fruity features");
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords!=banana", false, false);
        assertFalse(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFieldsAndSpecificField() {
        BibEntry e = new BibEntry(StandardEntryType.InProceedings);
        e.setField(StandardField.TITLE, "Fruity features");
        e.setField(StandardField.KEYWORDS, "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords=apple", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testIsMatch() {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "asdf");

        assertFalse(new SearchQuery("BiblatexEntryType", true, true).isMatch(entry));
        assertTrue(new SearchQuery("asdf", true, true).isMatch(entry));
        assertTrue(new SearchQuery("author=asdf", true, true).isMatch(entry));
    }

    @Test
    public void testIsValidQueryNotAsRegEx() {
        assertTrue(new SearchQuery("asdf", true, false).isValid());
    }

    @Test
    public void testIsValidQueryContainsBracketNotAsRegEx() {
        assertTrue(new SearchQuery("asdf[", true, false).isValid());
    }

    @Test
    public void testIsNotValidQueryContainsBracketNotAsRegEx() {
        assertTrue(new SearchQuery("asdf[", true, true).isValid());
    }

    @Test
    public void testIsValidQueryAsRegEx() {
        assertTrue(new SearchQuery("asdf", true, true).isValid());
    }

    @Test
    public void testIsValidQueryWithNumbersAsRegEx() {
        assertTrue(new SearchQuery("123", true, true).isValid());
    }

    @Test
    public void testIsValidQueryContainsBracketAsRegEx() {
        assertTrue(new SearchQuery("asdf[", true, true).isValid());
    }

    @Test
    public void testIsValidQueryWithEqualSignAsRegEx() {
        assertTrue(new SearchQuery("author=asdf", true, true).isValid());
    }

    @Test
    public void testIsValidQueryWithNumbersAndEqualSignAsRegEx() {
        assertTrue(new SearchQuery("author=123", true, true).isValid());
    }

    @Test
    public void testIsValidQueryWithEqualSignNotAsRegEx() {
        assertTrue(new SearchQuery("author=asdf", true, false).isValid());
    }

    @Test
    public void testIsValidQueryWithNumbersAndEqualSignNotAsRegEx() {
        assertTrue(new SearchQuery("author=123", true, false).isValid());
    }

    @Test
    public void isMatchedForNormalAndFieldBasedSearchMixed() {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "asdf");
        entry.setField(StandardField.ABSTRACT, "text");

        assertTrue(new SearchQuery("text AND author=asdf", true, true).isMatch(entry));
    }

    @Test
    public void testSimpleTerm() {
        String query = "progress";

        SearchQuery result = new SearchQuery(query, false, false);
        assertFalse(result.isGrammarBasedSearch());
    }

    @Test
    public void testGetPattern() {
        String query = "progress";
        SearchQuery result = new SearchQuery(query, false, false);
        Pattern pattern = Pattern.compile("(\\Qprogress\\E)");
        // We can't directly compare the pattern objects
        assertEquals(Optional.of(pattern.toString()), result.getPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testGetRegexpPattern() {
        String queryText = "[a-c]\\d* \\d*";
        SearchQuery regexQuery = new SearchQuery(queryText, false, true);
        Pattern pattern = Pattern.compile("([a-c]\\d* \\d*)");
        assertEquals(Optional.of(pattern.toString()), regexQuery.getPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testGetRegexpJavascriptPattern() {
        String queryText = "[a-c]\\d* \\d*";
        SearchQuery regexQuery = new SearchQuery(queryText, false, true);
        Pattern pattern = Pattern.compile("([a-c]\\d* \\d*)");
        assertEquals(Optional.of(pattern.toString()), regexQuery.getJavaScriptPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testEscapingInPattern() {
        // first word contain all java special regex characters
        String queryText = "<([{\\\\^-=$!|]})?*+.> word1 word2.";
        SearchQuery textQueryWithSpecialChars = new SearchQuery(queryText, false, false);
        String pattern = "(\\Q<([{\\^-=$!|]})?*+.>\\E)|(\\Qword1\\E)|(\\Qword2.\\E)";
        assertEquals(Optional.of(pattern), textQueryWithSpecialChars.getPatternForWords().map(Pattern::toString));
    }

    @Test
    public void testEscapingInJavascriptPattern() {
        // first word contain all javascript special regex characters that should be escaped individually in text based search
        String queryText = "([{\\\\^$|]})?*+./ word1 word2.";
        SearchQuery textQueryWithSpecialChars = new SearchQuery(queryText, false, false);
        String pattern = "(\\(\\[\\{\\\\\\^\\$\\|\\]\\}\\)\\?\\*\\+\\.\\/)|(word1)|(word2\\.)";
        assertEquals(Optional.of(pattern), textQueryWithSpecialChars.getJavaScriptPatternForWords().map(Pattern::toString));
    }
}
