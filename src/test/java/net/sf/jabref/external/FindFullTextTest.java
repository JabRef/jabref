package net.sf.jabref.external;

import net.sf.jabref.logic.fetcher.FullTextFinder;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

public class FindFullTextTest {
    private BibEntry entry;

    @Before
    public void setup() {
        entry = new BibEntry();
    }

    @After
    public void teardown() {
        entry = null;
    }

    @Test
    public void acceptPdfUrls() throws Exception {
        URL pdfUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FullTextFinder finder = (e) -> Optional.of(pdfUrl);
        FindFullText fetcher = new FindFullText(Arrays.asList(finder));

        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }

    @Test
    public void rejectNonPdfUrls() throws Exception {
        URL pdfUrl = new URL("https://github.com/JabRef/jabref/blob/master/README.md");
        FullTextFinder finder = (e) -> Optional.of(pdfUrl);
        FindFullText fetcher = new FindFullText(Arrays.asList(finder));

        assertEquals(Optional.empty(), fetcher.findFullTextPDF(entry));
    }
}
