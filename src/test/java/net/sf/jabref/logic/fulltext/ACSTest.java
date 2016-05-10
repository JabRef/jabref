package net.sf.jabref.logic.fulltext;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.support.DevEnvironment;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class ACSTest {

    private ACS finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        finder = new ACS();
        entry = new BibEntry();
    }

    @Test
    public void doiNotPresent() throws IOException {
        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test(expected = NullPointerException.class)
    public void rejectNullParameter() throws IOException {
        finder.findFullText(null);
        Assert.fail();
    }

    @Test
    public void findByDOI() throws IOException {
        // CI server is unreliable
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1021/bk-2006-STYG.ch014");

        Assert.assertEquals(
                Optional.of(new URL("http://pubs.acs.org/doi/pdf/10.1021/bk-2006-STYG.ch014")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void notFoundByDOI() throws IOException {
        // CI server is unreliable
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1021/bk-2006-WWW.ch014");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
