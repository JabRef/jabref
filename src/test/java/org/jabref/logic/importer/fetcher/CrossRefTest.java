package org.jabref.logic.importer.fetcher;

import java.util.Locale;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
public class CrossRefTest {

    private CrossRef fetcher;
    private BibEntry barrosEntry;

    @BeforeEach
    public void setUp() throws Exception {
        fetcher = new CrossRef();

        barrosEntry = new BibEntry();
        barrosEntry.setField("title", "Service Interaction Patterns");
        barrosEntry.setField("author", "Alistair Barros and Marlon Dumas and Arthur H. M. ter Hofstede");
        barrosEntry.setField("year", "2005");
        barrosEntry.setField("doi", "10.1007/11538394_20");
        barrosEntry.setField("issn", "0302-9743");
        barrosEntry.setField("pages", "302-318");
    }

    @Test
    public void findExactData() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Service Interaction Patterns");
        entry.setField("author", "Barros, Alistair and Dumas, Marlon and Arthur H.M. ter Hofstede");
        entry.setField("year", "2005");
        assertEquals("10.1007/11538394_20", fetcher.findIdentifier(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findMissingAuthor() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        entry.setField("author", "Stefan Kolb");
        assertEquals("10.1109/sose.2014.26", fetcher.findIdentifier(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findTitleOnly() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        assertEquals("10.1109/sose.2014.26", fetcher.findIdentifier(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void notFindIncompleteTitle() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability");
        entry.setField("author", "Stefan Kolb and Guido Wirtz");
        assertEquals(Optional.empty(), fetcher.findIdentifier(entry));
    }

    @Test
    public void acceptTitleUnderThreshold() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service----");
        entry.setField("author", "Stefan Kolb and Guido Wirtz");
        assertEquals("10.1109/sose.2014.26", fetcher.findIdentifier(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void notAcceptTitleOverThreshold() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service-----");
        entry.setField("author", "Stefan Kolb and Guido Wirtz");
        assertEquals(Optional.empty(), fetcher.findIdentifier(entry));
    }

    @Test
    public void findWrongAuthor() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        entry.setField("author", "Stefan Kolb and Simon Harrer");
        assertEquals("10.1109/sose.2014.26", fetcher.findIdentifier(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findWithSubtitle() throws Exception {
        BibEntry entry = new BibEntry();
        // CrossRef entry will only include { "title": "A break in the clouds", "subtitle": "towards a cloud definition" }
        entry.setField("title", "A break in the clouds: towards a cloud definition");
        assertEquals("10.1145/1496091.1496100", fetcher.findIdentifier(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findByDOI() throws Exception {
        assertEquals(Optional.of(barrosEntry), fetcher.performSearchById("10.1007/11538394_20"));
    }

    @Test
    public void findByAuthors() throws Exception {
        assertEquals(Optional.of(barrosEntry), fetcher.performSearch("Barros, Alistair and Dumas, Marlon and Arthur H.M. ter Hofstede").stream().findFirst());
    }

    @Test
    public void findByEntry() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Service Interaction Patterns");
        entry.setField("author", "Barros, Alistair and Dumas, Marlon and Arthur H.M. ter Hofstede");
        entry.setField("year", "2005");
        assertEquals(Optional.of(barrosEntry), fetcher.performSearch(entry).stream().findFirst());
    }

    @Test
    public void performSearchByIdFindsPaperWithoutTitle() throws Exception {
        BibEntry entry = new BibEntry("article");
        entry.setField("author", "Dominik Wujastyk");
        entry.setField("doi", "10.1023/a:1003473214310");
        entry.setField("issn", "0019-7246");
        entry.setField("pages", "172-176");
        entry.setField("volume", "42");
        entry.setField("year", "1999");

        assertEquals(Optional.of(entry), fetcher.performSearchById("10.1023/a:1003473214310"));
    }
}
