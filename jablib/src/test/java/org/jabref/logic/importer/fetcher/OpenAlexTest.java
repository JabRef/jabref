package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.ProgressInputStream;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class OpenAlexTest {
    @Test
    void findFullText_returnsPdfUrl_fromMockedOpenAlexResponse() throws Exception {
        // Entry with DOI, so OpenAlex will build an API URL from it
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1145/3503250");

        // JSON response containing primary_location.pdf_url
        String json = """
            {
              "primary_location": {
                "pdf_url": "https://example.org/paper.pdf"
              }
            }
            """;

        // Mock URLDownload to return our JSON as a stream
        URLDownload download = mock(URLDownload.class);

        // Create a ProgressInputStream backed by our JSON bytes.
        ProgressInputStream stream = new ProgressInputStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), 1024
        );

        when(download.asInputStream()).thenReturn(stream);

        // Spy OpenAlex so we can intercept the network call
        OpenAlex fetcher = spy(new OpenAlex());

        // Stub the internal download method so NO real HTTP occurs
        doReturn(download).when(fetcher).getUrlDownload(any(URL.class));

        Optional<URL> result = fetcher.findFullText(entry);

        assertEquals(Optional.of(new URL("https://example.org/paper.pdf")), result);

        // verify network layer was invoked exactly once
        verify(fetcher, times(1)).getUrlDownload(any(URL.class));
        verify(download, times(1)).asInputStream();
    }

    @Test
    void findFullText_returnsEmpty_whenPdfUrlMissing() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1145/3503250");

        String json = """
            {
              "primary_location": { }
            }
            """;

        URLDownload download = mock(URLDownload.class);
        ProgressInputStream stream = new ProgressInputStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), 1024
        );
        when(download.asInputStream()).thenReturn(stream);

        OpenAlex fetcher = spy(new OpenAlex());
        doReturn(download).when(fetcher).getUrlDownload(any(URL.class));

        Optional<URL> result = fetcher.findFullText(entry);

        assertEquals(Optional.empty(), result);
        verify(fetcher, times(1)).getUrlDownload(any(URL.class));
    }

    @Test
    void findFullText_throwsFetcherException_whenDownloadFails() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1145/3503250");

        URLDownload download = mock(URLDownload.class);
        when(download.asInputStream()).thenThrow(new RuntimeException("boom"));

        OpenAlex fetcher = spy(new OpenAlex());
        doReturn(download).when(fetcher).getUrlDownload(any(URL.class));

        assertThrows(FetcherException.class, () -> fetcher.findFullText(entry));
        verify(fetcher, times(1)).getUrlDownload(any(URL.class));
    }
}
