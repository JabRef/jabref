package org.jabref.logic.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FulltextFetchersTest {
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        entry = new BibEntry();
    }

    @AfterEach
    public void tearDown() {
        entry = null;
    }

    @Test
    public void acceptPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Arrays.asList(finder));

        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void rejectNonPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("https://github.com/JabRef/jabref/blob/master/README.md");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Arrays.asList(finder));

        assertEquals(Optional.empty(), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void noTrustLevel() throws MalformedURLException {
        URL pdfUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Arrays.asList(finder));

        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void higherTrustLevelWins() throws MalformedURLException {
        final URL lowUrl = new URL("http://docs.oasis-open.org/opencsa/sca-bpel/sca-bpel-1.1-spec-cd-01.pdf");
        final URL highUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FulltextFetcher finderHigh = new FulltextFetcher() {
            @Override
            public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
                return Optional.of(highUrl);
            }

            @Override
            public TrustLevel getTrustLevel() {
                return TrustLevel.SOURCE;
            }
        };
        FulltextFetcher finderLow = new FulltextFetcher() {
            @Override
            public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
                return Optional.of(lowUrl);
            }

            @Override
            public TrustLevel getTrustLevel() {
                return TrustLevel.UNKNOWN;
            }
        };
        FulltextFetchers fetcher = new FulltextFetchers(Arrays.asList(finderLow, finderHigh));

        assertEquals(Optional.of(highUrl), fetcher.findFullTextPDF(entry));
    }
}
