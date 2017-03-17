package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.support.DevEnvironment;
import org.jabref.testutils.category.FetcherTests;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(FetcherTests.class)
public class ScienceDirectTest {

    private ScienceDirect finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        finder = new ScienceDirect();
        entry = new BibEntry();
    }

    @Test(expected = NullPointerException.class)
    public void rejectNullParameter() throws IOException {
        finder.findFullText(null);
        Assert.fail();
    }

    @Test
    public void doiNotPresent() throws IOException {
        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void findByDOIOldPage() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1016/j.jrmge.2015.08.004");

        Assert.assertEquals(
                Optional.of(new URL("http://www.sciencedirect.com/science/article/pii/S1674775515001079/pdfft?md5=2b19b19a387cffbae237ca6a987279df&pid=1-s2.0-S1674775515001079-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void findByDOINewPage() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1016/j.aasri.2014.09.002");

        Assert.assertEquals(
                Optional.of(new URL("http://www.sciencedirect.com/science/article/pii/S2212671614001024/pdf?md5=4e2e9a369b4d5b3db5100aba599bef8b&pid=1-s2.0-S2212671614001024-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void notFoundByDOI() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1016/j.aasri.2014.0559.002");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
