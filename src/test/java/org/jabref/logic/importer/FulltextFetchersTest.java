package org.jabref.logic.importer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FulltextFetchersTest {
    private BibEntry entry;

    @Before
    public void setUp() {
        entry = new BibEntry();
    }

    @After
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
}
