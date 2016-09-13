package net.sf.jabref.logic.search;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.IdGenerator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchQueryTest {

    @Test
    public void testToString() {
        assertEquals("\"asdf\" (case sensitive, regular expression)", new SearchQuery("asdf", true, true).toString());
        assertEquals("\"asdf\" (case insensitive, plain text)", new SearchQuery("asdf", false, false).toString());
    }

    @Test
    public void testIsContainsBasedSearch() {
        assertTrue(new SearchQuery("asdf", true, false).isContainsBasedSearch());
        assertFalse(new SearchQuery("asdf", true, true).isContainsBasedSearch());
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
        entry.addKeyword("one two", ", ");
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
        BibEntry e = new BibEntry(IdGenerator.next(), BibtexEntryTypes.INPROCEEDINGS.getName());
        e.setField(FieldName.BOOKTITLE, "Super Conference (SC)");

        SearchQuery searchQuery = new SearchQuery("booktitle=\"(\"", false, false);
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
    public void testIsValidQuery() {
        assertFalse(new SearchQuery("asdf[", true, true).isValidQuery());
        assertTrue(new SearchQuery("asdf[", true, false).isValidQuery());
        assertTrue(new SearchQuery("asdf", true, false).isValidQuery());
        assertTrue(new SearchQuery("asdf", true, true).isValidQuery());
        assertTrue(new SearchQuery("123", true, true).isValidQuery());
        assertTrue(new SearchQuery("123", true, true).isValidQuery());
        assertTrue(new SearchQuery("author=asdf", true, false).isValidQuery());
        assertTrue(new SearchQuery("author=asdf", true, true).isValidQuery());
        assertTrue(new SearchQuery("author=123", true, false).isValidQuery());
        assertTrue(new SearchQuery("author=123", true, true).isValidQuery());
    }

}
