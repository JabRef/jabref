package net.sf.jabref.logic.fulltext;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.support.DevEnvironment;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

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
    public void findByDOI() throws IOException {
        // CI server is blocked
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1016/j.jrmge.2015.08.004");

        Assert.assertEquals(
                Optional.of(new URL("http://www.sciencedirect.com/science/article/pii/S1674775515001079/pdfft?md5=2b19b19a387cffbae237ca6a987279df&pid=1-s2.0-S1674775515001079-main.pdf")),
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
