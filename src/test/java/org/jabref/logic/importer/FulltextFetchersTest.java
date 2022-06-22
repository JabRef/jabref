package org.jabref.logic.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class FulltextFetchersTest {

    private BibEntry entry = new BibEntry();

    @Test
    public void acceptPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Set.of(finder));
        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void rejectNonPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("https://github.com/JabRef/jabref/blob/master/README.md");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Set.of(finder));

        assertEquals(Optional.empty(), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void noTrustLevel() throws MalformedURLException {
        URL pdfUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Set.of(finder));

        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void higherTrustLevelWins() throws IOException, FetcherException {
        FulltextFetcher finderHigh = mock(FulltextFetcher.class);
        when(finderHigh.getTrustLevel()).thenReturn(TrustLevel.SOURCE);
        final URL highUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        when(finderHigh.findFullText(entry)).thenReturn(Optional.of(highUrl));

        FulltextFetcher finderLow = mock(FulltextFetcher.class);
        when(finderLow.getTrustLevel()).thenReturn(TrustLevel.UNKNOWN);
        final URL lowUrl = new URL("http://docs.oasis-open.org/opencsa/sca-bpel/sca-bpel-1.1-spec-cd-01.pdf");
        when(finderLow.findFullText(entry)).thenReturn(Optional.of(lowUrl));

        FulltextFetchers fetcher = new FulltextFetchers(Set.of(finderLow, finderHigh));

        // set an (arbitrary) DOI to the test entry to skip side effects inside the "findFullTextPDF" method
        entry.setField(StandardField.DOI, "10.5220/0007903201120130");

        assertEquals(Optional.of(highUrl), fetcher.findFullTextPDF(entry));
    }
}
