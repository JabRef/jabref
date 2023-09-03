package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@FetcherTest
public class RfcFetcherTest {

    private RfcFetcher fetcher = new RfcFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    private BibEntry bibEntry = new BibEntry(StandardEntryType.Misc)
            .withCitationKey("rfc1945")
            .withField(StandardField.SERIES, "Request for Comments")
            .withField(StandardField.NUMBER, "1945")
            .withField(StandardField.HOWPUBLISHED, "RFC 1945")
            .withField(StandardField.PUBLISHER, "RFC Editor")
            .withField(StandardField.DOI, "10.17487/RFC1945")
            .withField(StandardField.URL, "https://www.rfc-editor.org/info/rfc1945")
            .withField(StandardField.AUTHOR, "Henrik Nielsen and Roy T. Fielding and Tim Berners-Lee")
            .withField(StandardField.TITLE, "{Hypertext Transfer Protocol -- HTTP/1.0}")
            .withField(StandardField.PAGETOTAL, "60")
            .withField(StandardField.YEAR, "1996")
            .withField(StandardField.MONTH, "#may#")
            .withField(StandardField.ABSTRACT, "The Hypertext Transfer Protocol (HTTP) is an application-level protocol with the lightness and speed necessary for distributed, collaborative, hypermedia information systems. This memo provides information for the Internet community. This memo does not specify an Internet standard of any kind.");

    @Test
    public void getNameReturnsEqualIdName() {
        assertEquals("RFC", fetcher.getName());
    }

    @Test
    public void performSearchByIdFindsEntryWithDraftIdentifier() throws Exception {
        BibEntry bibDraftEntry = new BibEntry(StandardEntryType.TechReport)
                .withField(InternalField.KEY_FIELD, "fielding-http-spec-01")
                .withField(StandardField.AUTHOR, "Henrik Nielsen and Roy T. Fielding and Tim Berners-Lee")
                .withField(StandardField.DAY, "20")
                .withField(StandardField.INSTITUTION, "Internet Engineering Task Force")
                .withField(StandardField.MONTH, "#dec#")
                .withField(StandardField.NOTE, "Work in Progress")
                .withField(StandardField.NUMBER, "draft-fielding-http-spec-01")
                .withField(StandardField.PAGETOTAL, "41")
                .withField(StandardField.PUBLISHER, "Internet Engineering Task Force")
                .withField(StandardField.TITLE, "{Hypertext Transfer Protocol -- HTTP/1.0}")
                .withField(StandardField.TYPE, "Internet-Draft")
                .withField(StandardField.URL, "https://datatracker.ietf.org/doc/draft-fielding-http-spec/01/")
                .withField(StandardField.YEAR, "1994")
                .withField(StandardField.ABSTRACT, "The Hypertext Transfer Protocol (HTTP) is an application-level protocol with the lightness and speed necessary for distributed, collaborative, hypermedia information systems. It is a generic, stateless, object-oriented protocol which can be used for many tasks, such as name servers and distributed object management systems, through extension of its request methods (commands). A feature of HTTP is the typing and negotiation of data representation, allowing systems to be built independently of the data being transferred. HTTP has been in use by the World-Wide Web global information initiative since 1990. This specification reflects preferred usage of the protocol referred to as 'HTTP/1.0', and is compatible with the most commonly used HTTP server and client programs implemented prior to November 1994.");
        bibDraftEntry.setCommentsBeforeEntry("%% You should probably cite draft-ietf-http-v10-spec instead of this I-D.\n");

        assertEquals(Optional.of(bibDraftEntry), fetcher.performSearchById("draft-fielding-http-spec"));
    }

    @ParameterizedTest
    @CsvSource({"rfc1945", "RFC1945", "1945"})
    public void performSearchByIdFindsEntry(String identifier) throws Exception {
        assertEquals(Optional.of(bibEntry), fetcher.performSearchById(identifier));
    }

    @Test
    public void performSearchByIdFindsNothingWithoutIdentifier() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById(""));
    }

    @ParameterizedTest
    @CsvSource({
            // syntactically valid identifier
            "draft-test-draft-spec",
            "RFC9999",
            // invalid identifier
            "banana"})
    public void performSearchByIdFindsNothingWithValidDraftIdentifier(String identifier) throws Exception {
        assertThrows(FetcherClientException.class, () -> fetcher.performSearchById(identifier));
    }
}
