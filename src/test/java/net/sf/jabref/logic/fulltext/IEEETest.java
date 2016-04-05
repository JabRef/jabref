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

public class IEEETest {
    private IEEE finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        finder = new IEEE();
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

        entry.setField("doi", "10.1109/ACCESS.2016.2535486");

        Assert.assertEquals(
                Optional.of(
                        new URL("http://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    public void findByURL() throws IOException {
        // CI server is unreliable
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("url", "http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7421926");

        Assert.assertEquals(
                Optional.of(
                        new URL("http://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    public void findByOldURL() throws IOException {
        // CI server is unreliable
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("url", "http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=7421926");

        Assert.assertEquals(
                Optional.of(
                        new URL("http://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    public void findByDOIButNotURL() throws IOException {
        // CI server is unreliable
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("doi", "10.1109/ACCESS.2016.2535486");
        entry.setField("url", "http://dx.doi.org/10.1109/ACCESS.2016.2535486");

        Assert.assertEquals(
                Optional.of(
                        new URL("http://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931")),
                finder.findFullText(entry));
    }

    @Test
    public void notFoundByURL() throws IOException {
        // CI server is unreliable
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("url", "http://dx.doi.org/10.1109/ACCESS.2016.2535486");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1021/bk-2006-WWW.ch014");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
