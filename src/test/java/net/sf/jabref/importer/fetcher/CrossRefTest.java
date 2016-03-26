package net.sf.jabref.importer.fetcher;

import net.sf.jabref.model.entry.BibEntry;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class CrossRefTest {
    @Test
    public void findExactData() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Service Interaction Patterns");
        entry.setField("author", "Barros, Alistair and Dumas, Marlon and Arthur H.M. ter Hofstede");
        entry.setField("year", "2005");
        assertEquals("10.1007/11538394_20", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findMissingAuthor() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        entry.setField("author", "Stefan Kolb");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findTitleOnly() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findIncompleteTitle() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability");
        entry.setField("author", "Stefan Kolb and Guido Wirtz");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void findWrongAuthor() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Towards Application Portability in Platform as a Service");
        entry.setField("author", "Stefan Kolb and Simon Harrer");
        assertEquals("10.1109/sose.2014.26", CrossRef.findDOI(entry).get().getDOI().toLowerCase(Locale.ENGLISH));
    }
}