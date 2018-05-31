package org.jabref.logic.search;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

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
        entry.setField(FieldName.TITLE, "systematic revie");
        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", false, false);
        assertFalse(searchQuery.isMatch(entry));
    }

    @Test
    public void testGrammarSearchFullEntry() {
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.TITLE, "systematic review");
        SearchQuery searchQuery = new SearchQuery("title=\"systematic review\"", false, false);
        assertTrue(searchQuery.isMatch(entry));
    }

    @Test
    public void testSearchingForOpenBraketInBooktitle() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());
        e.setField(FieldName.BOOKTITLE, "Super Conference (SC)");

        SearchQuery searchQuery = new SearchQuery("booktitle=\"(\"", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchMatchesSingleKeywordNotPart() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());
        e.setField("keywords", "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anykeyword==apple", false, false);
        assertFalse(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchMatchesSingleKeyword() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());
        e.setField("keywords", "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anykeyword==pineapple", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFields() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());
        e.setField("title", "Fruity features");
        e.setField("keywords", "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield==\"fruity features\"", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFieldsNotForSpecificField() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());
        e.setField("title", "Fruity features");
        e.setField("keywords", "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords!=banana", false, false);
        assertFalse(searchQuery.isMatch(e));
    }

    @Test
    public void testSearchAllFieldsAndSpecificField() {
        BibEntry e = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());
        e.setField("title", "Fruity features");
        e.setField("keywords", "banana, pineapple, orange");

        SearchQuery searchQuery = new SearchQuery("anyfield=fruit and keywords=apple", false, false);
        assertTrue(searchQuery.isMatch(e));
    }

    @Test
    public void testIsMatch() {
        BibEntry entry = new BibEntry();
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "asdf");

        assertFalse(new SearchQuery("qwer", true, true).isMatch(entry));
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
        entry.setType(BibtexEntryTypes.ARTICLE);
        entry.setField("author", "asdf");
        entry.setField("abstract", "text");

        assertTrue(new SearchQuery("text AND author=asdf", true, true).isMatch(entry));

    }

    @Test
    public void testSimpleTerm() {
        String query = "progress";

        SearchQuery result = new SearchQuery(query, false, false);

        assertFalse(result.isGrammarBasedSearch());
    }

}
