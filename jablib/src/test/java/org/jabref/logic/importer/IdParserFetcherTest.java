package org.jabref.logic.importer;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.net.ProgressInputStream;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.http.SimpleHttpResponse;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class IdParserFetcherTest {

    @Test
    void usesUrlDownloadForIdentifierLookups() throws FetcherException, MalformedURLException, URISyntaxException, ParseException {
        URL url = URLUtil.create("https://example.org");
        URLDownload urlDownload = mock(URLDownload.class);
        Parser parser = mock(Parser.class);
        ProgressInputStream stream = new ProgressInputStream(new ByteArrayInputStream(new byte[0]), 0);
        BibEntry fetchedEntry = new BibEntry();
        fetchedEntry.setField(StandardField.DOI, "10.1000/example");
        when(urlDownload.asInputStream()).thenReturn(stream);
        when(parser.parseEntries(stream)).thenReturn(List.of(fetchedEntry));
        IdParserFetcher<DOI> fetcher = mockFetcher(url, urlDownload, parser);
        when(fetcher.extractIdentifier(any(BibEntry.class), eq(List.of(fetchedEntry)))).thenReturn(DOI.parse("10.1000/example"));

        assertEquals(DOI.parse("10.1000/example"), fetcher.findIdentifier(new BibEntry()));
    }

    @Test
    void returnsEmptyWhenIdentifierLookupReturnsNotFound() throws FetcherException, MalformedURLException, URISyntaxException {
        URL url = URLUtil.create("https://example.org");
        URLDownload urlDownload = mock(URLDownload.class);
        when(urlDownload.asInputStream()).thenThrow(new FetcherClientException(
                url,
                new SimpleHttpResponse(HttpURLConnection.HTTP_NOT_FOUND, "Not Found", "")));
        IdParserFetcher<DOI> fetcher = mockFetcher(url, urlDownload, mock(Parser.class));

        assertEquals(Optional.empty(), fetcher.findIdentifier(new BibEntry()));
    }

    @SuppressWarnings("unchecked")
    private IdParserFetcher<DOI> mockFetcher(URL url, URLDownload urlDownload, Parser parser) throws MalformedURLException, URISyntaxException, FetcherException {
        IdParserFetcher<DOI> fetcher = mock(IdParserFetcher.class, Answers.CALLS_REAL_METHODS);
        when(fetcher.getURLForEntry(any(BibEntry.class))).thenReturn(url);
        when(fetcher.getParser()).thenReturn(parser);
        when(fetcher.getUrlDownload(url)).thenReturn(urlDownload);
        return fetcher;
    }
}
