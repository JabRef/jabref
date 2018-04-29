package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
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
    private BibEntry bibEntry;

    @BeforeEach
    public void setUp() {
        fetcher = new RfcFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.MISC);
        bibEntry.setField("series", "Request for Comments");
        bibEntry.setField("number", "1945");
        bibEntry.setField("howpublished", "RFC 1945");
        bibEntry.setField("publisher", "RFC Editor");
        bibEntry.setField("doi", "10.17487/RFC1945");
        bibEntry.setField("url", "https://rfc-editor.org/rfc/rfc1945.txt");
        bibEntry.setField("author", "Henrik Frystyk Nielsen and Roy T. Fielding and Tim Berners-Lee");
        bibEntry.setField("title", "{Hypertext Transfer Protocol -- HTTP/1.0}");
        bibEntry.setField("pagetotal", "60");
        bibEntry.setField("year", "1996");
        bibEntry.setField("month", "#may#");
        bibEntry.setField("abstract", "The Hypertext Transfer Protocol (HTTP) is an application-level protocol with the lightness and speed necessary for distributed, collaborative, hypermedia information systems. This memo provides information for the Internet community. This memo does not specify an Internet standard of any kind.");
        bibEntry.setCiteKey("rfc1945");
    }

    @Test
    public void getNameReturnsEqualIdName() {
        assertEquals("RFC", fetcher.getName());
    }

    @Test
    public void getHelpPageReturnsEqualHelpPage() {
        assertEquals("RFCtoBibTeX", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void performSearchByIdFindsEntryWithRfcPrefix() throws Exception {
        assertEquals(Optional.of(bibEntry), fetcher.performSearchById("RFC1945"));
    }

    @Test
    public void performSearchByIdFindsEntryWithoutRfcPrefix() throws Exception {
        assertEquals(Optional.of(bibEntry), fetcher.performSearchById("1945"));
    }

    @Test
    public void performSearchByIdFindsNothingWithoutIdentifier() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById(""));
    }

    @Test
    public void performSearchByIdFindsNothingWithValidIdentifier() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById("RFC9999"));
    }

    @Test
    public void performSearchByIdFindsNothingWithInvalidIdentifier() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById("banana"));
    }
}
