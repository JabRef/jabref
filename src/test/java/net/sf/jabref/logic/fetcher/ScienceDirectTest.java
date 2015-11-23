package net.sf.jabref.logic.fetcher;

import net.sf.jabref.model.entry.BibtexEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

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

    @Test
    public void doiNotPresent() throws IOException {
        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void findByDOI() throws IOException {
        entry.setField("doi", "10.1016/j.aasri.2014.09.002");

        Assert.assertEquals(
                Optional.of(new URL("http://api.elsevier.com/content/article/doi/10.1016/j.aasri.2014.09.002?httpAccept=application/pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1016/j.aasri.2014.0559.002");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
