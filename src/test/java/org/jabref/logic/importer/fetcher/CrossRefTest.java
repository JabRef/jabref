package org.jabref.logic.importer.fetcher;

import java.util.Locale;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

@Category(FetcherTests.class)
public class CrossRefTest {
    @Test
    public void findExactData() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Service Interaction Patterns");
        entry.setField("author", "Barros, Alistair and Dumas, Marlon and Arthur H.M. ter Hofstede");
        entry.setField("year", "2005");
        assertEquals("10.1007/11538394_20", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findMissingAuthor() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        entry.setField("author", "Stefan Kolb");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findTitleOnly() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void notFindIncompleteTitle() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability");
        entry.setField("author", "Stefan Kolb and Guido Wirtz");
        assertEquals(Optional.empty(), CrossRef.findDOI(entry));
    }

    @Test
    public void acceptTitleUnderThreshold() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service----");
        entry.setField("author", "Stefan Kolb and Guido Wirtz");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void notAcceptTitleOverThreshold() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service-----");
        entry.setField("author", "Stefan Kolb and Guido Wirtz");
        assertEquals(Optional.empty(), CrossRef.findDOI(entry));
    }

    @Test
    public void findWrongAuthor() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        entry.setField("author", "Stefan Kolb and Simon Harrer");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }
}
