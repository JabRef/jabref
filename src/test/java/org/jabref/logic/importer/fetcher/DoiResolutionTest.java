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
public class DoiResolutionTest {

    private DoiResolution finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        finder = new DoiResolution();
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
    public void findByDOI() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1051/0004-6361/201527330");

        Assert.assertEquals(
                Optional.of(new URL("http://www.aanda.org/articles/aa/pdf/2016/01/aa27330-15.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void notReturnAnythingWhenMultipleLinksAreFound() throws IOException {
        entry.setField("doi", "10.1051/0004-6361/201527330; 10.1051/0004-6361/20152711233");
        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void notFoundByDOI() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1186/unknown-doi");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
