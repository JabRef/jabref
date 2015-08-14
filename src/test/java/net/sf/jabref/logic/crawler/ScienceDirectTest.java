package net.sf.jabref.logic.crawler;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.logic.crawler.ScienceDirect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class ScienceDirectTest {
    ScienceDirect finder;
    BibtexEntry entry;

    @Before
    public void setup() {
        finder = new ScienceDirect();
        entry = new BibtexEntry();
    }


    @Test(expected = NullPointerException.class)
    public void rejectNullParameter() throws IOException {
        finder.findFullText(null);
    }

    /*
    @Test
    public void findFullTextURL() throws IOException {
        Assert.assertEquals(
                new URL("http://www.sciencedirect.com/science/article/pii/S2212671614001024/pdf?md5=4e2e9a369b4d5b3db5100aba599bef8b&pid=1-s2.0-S2212671614001024-main.pdf"),
                finder.findFullTextURL(new URL("http://www.sciencedirect.com/science/article/pii/S2212671614001024"))
        );
    }

    @Test
    public void nullWithNoFullTextURL() throws IOException {
        Assert.assertNull(finder.findFullTextURL(new URL("http://www.sciencedirect.com/science/journals")));
    }
    */
}
