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
        URL pdfUrl = new URL("https://www.uni-bamberg.de/fileadmin/uni/fakultaeten/wiai_lehrstuehle/praktische_informatik/Dateien/Publikationen/cloud15-application-migration-effort-in-the-cloud.pdf");
        FullTextFinder finder = (e) -> Optional.of(pdfUrl);
        FindFullText fetcher = new FindFullText(Arrays.asList(finder));

        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }
}