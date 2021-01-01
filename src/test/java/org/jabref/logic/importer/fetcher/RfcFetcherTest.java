package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class RfcFetcherTest {

    private RfcFetcher fetcher;
    private BibEntry bibEntry;

    @BeforeEach
    public void setUp() {
        fetcher = new RfcFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibEntry = new BibEntry();
        bibEntry.setType(StandardEntryType.Misc);
        bibEntry.setField(StandardField.SERIES, "Request for Comments");
        bibEntry.setField(StandardField.NUMBER, "1945");
        bibEntry.setField(StandardField.HOWPUBLISHED, "RFC 1945");
        bibEntry.setField(StandardField.PUBLISHER, "RFC Editor");
        bibEntry.setField(StandardField.DOI, "10.17487/RFC1945");
        bibEntry.setField(StandardField.URL, "https://rfc-editor.org/rfc/rfc1945.txt");
        bibEntry.setField(StandardField.AUTHOR, "Henrik Nielsen and Roy T. Fielding and Tim Berners-Lee");
        bibEntry.setField(StandardField.TITLE, "{Hypertext Transfer Protocol -- HTTP/1.0}");
        bibEntry.setField(StandardField.PAGETOTAL, "60");
        bibEntry.setField(StandardField.YEAR, "1996");
        bibEntry.setField(StandardField.MONTH, "#may#");
        bibEntry.setField(StandardField.ABSTRACT, "The Hypertext Transfer Protocol (HTTP) is an application-level protocol with the lightness and speed necessary for distributed, collaborative, hypermedia information systems. This memo provides information for the Internet community. This memo does not specify an Internet standard of any kind.");
        bibEntry.setCitationKey("rfc1945");
    }

    @Test
    public void getNameReturnsEqualIdName() {
        assertEquals("RFC", fetcher.getName());
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
