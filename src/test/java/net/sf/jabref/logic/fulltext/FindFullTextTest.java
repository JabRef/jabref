package net.sf.jabref.logic.fulltext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FindFullTextTest {
    private BibEntry entry;

    @Before
    public void setUp() {
        entry = new BibEntry();
    }

    @After
    public void tearDown() {
        entry = null;
    }

    @Ignore
    @Test
    public void acceptPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FullTextFinder finder = (e) -> Optional.of(pdfUrl);
        FindFullText fetcher = new FindFullText(Arrays.asList(finder));

        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void rejectNonPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("https://github.com/JabRef/jabref/blob/master/README.md");
        FullTextFinder finder = (e) -> Optional.of(pdfUrl);
        FindFullText fetcher = new FindFullText(Arrays.asList(finder));

        assertEquals(Optional.empty(), fetcher.findFullTextPDF(entry));
    }
}
