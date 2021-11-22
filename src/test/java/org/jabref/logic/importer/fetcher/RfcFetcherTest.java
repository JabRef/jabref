package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
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
    public void performSearchByIdFindsEntryWithDraftIdentifier() throws Exception {
        BibEntry bibDraftEntry = new BibEntry();
        bibDraftEntry.setType(StandardEntryType.TechReport);
        bibDraftEntry.setField(InternalField.KEY_FIELD, "fielding-http-spec-01");
        bibDraftEntry.setField(StandardField.AUTHOR, "Henrik Nielsen and Roy T. Fielding and Tim Berners-Lee");
        bibDraftEntry.setField(StandardField.DAY, "20");
        bibDraftEntry.setField(StandardField.INSTITUTION, "Internet Engineering Task Force");
        bibDraftEntry.setField(StandardField.MONTH, "#dec#");
        bibDraftEntry.setField(StandardField.NOTE, "Work in Progress");
        bibDraftEntry.setField(StandardField.NUMBER, "draft-fielding-http-spec-01");
        bibDraftEntry.setField(StandardField.PAGETOTAL, "41");
        bibDraftEntry.setField(StandardField.PUBLISHER, "Internet Engineering Task Force");
        bibDraftEntry.setField(StandardField.TITLE, "{Hypertext Transfer Protocol -- HTTP/1.0}");
        bibDraftEntry.setField(StandardField.TYPE, "Internet-Draft");
        bibDraftEntry.setField(StandardField.URL, "https://datatracker.ietf.org/doc/html/draft-fielding-http-spec-01");
        bibDraftEntry.setField(StandardField.YEAR, "1994");
        bibDraftEntry.setField(StandardField.ABSTRACT, "The Hypertext Transfer Protocol (HTTP) is an application-level protocol with the lightness and speed necessary for distributed, collaborative, hypermedia information systems. It is a generic, stateless, object-oriented protocol which can be used for many tasks, such as name servers and distributed object management systems, through extension of its request methods (commands). A feature of HTTP is the typing and negotiation of data representation, allowing systems to be built independently of the data being transferred. HTTP has been in use by the World-Wide Web global information initiative since 1990. This specification reflects preferred usage of the protocol referred to as 'HTTP/1.0', and is compatible with the most commonly used HTTP server and client programs implemented prior to November 1994.");
        bibDraftEntry.setCommentsBeforeEntry("%% You should probably cite draft-ietf-http-v10-spec instead of this I-D.\n");

        assertEquals(Optional.of(bibDraftEntry), fetcher.performSearchById("draft-fielding-http-spec"));
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
    public void performSearchByIdFindsNothingWithValidDraftIdentifier() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById("draft-test-draft-spec"));
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
