package net.sf.jabref.logic.search;

import net.sf.jabref.model.entry.BibEntry;

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
        entry.addKeyword("one two");
        SearchQuery searchQuery = new SearchQuery("keywords=\"one two\"", false, false);
        assertTrue(searchQuery.isMatch(entry));
    }

}