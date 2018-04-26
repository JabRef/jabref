package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@FetcherTest
public class RfcFetcherTest {
    
    private RfcFetcher fetcher;

    @BeforeEach
    public void setUp() {
        fetcher = new RfcFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }
    
    @Test
    public void testGetName() {
        assertEquals("RfcFetcher", fetcher.getName());
    }
    
    /*
    Test for when 'RfcFetcher to Bibtex' help page is added
    
    @Test
    public void testGetHelpPage() {
        assertEquals(HelpFile.FETCHER_DIVA, HelpFile.FETCHER_DIVA);
    }
    */
    
    @Test
    public void performSearchById() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType("misc");
        entry.setField("series", "{Request for Comments}");
        entry.setField("number", "1945");
        entry.setField("howpublished", "{RfcFetcher 1945}");
        entry.setField("publisher", "{RfcFetcher Editor}");
        entry.setField("doi", "{10.17487/RFC1945}");
        entry.setField("url", "{https://rfc-editor.org/rfc/rfc1945.txt}");
        entry.setField("author", "{Henrik Frystyk Nielsen and Roy T. Fielding and Tim Berners-Lee}");
        entry.setField("title", "{{Hypertext Transfer Protocol -- HTTP/1.0}}");
        entry.setField("pagetotal", "60");
        entry.setField("year", "1994");
        entry.setField("month", "may");
        entry.setField("abstract", "{The Hypertext Transfer Protocol (HTTP) is an application-level protocol with the lightness and speed necessary for distributed, collaborative, hypermedia information systems. This memo provides information for the Internet community. This memo does not specify an Internet standard of any kind.}");
        entry.setCiteKey("Berners-Lee1996");
        
        assertEquals(Optional.of(entry), fetcher.performSearchById("1945"));
    }
}
