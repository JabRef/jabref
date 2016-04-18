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

public class GoogleScholarTest {

    private GoogleScholar finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        finder = new GoogleScholar();
        entry = new BibEntry();
    }

    @Test(expected = NullPointerException.class)
    public void rejectNullParameter() throws IOException {
        finder.findFullText(null);
        Assert.fail();
    }

    @Test
    public void requiresEntryTitle() throws IOException {
        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void linkFound() throws IOException {
        // CI server is blocked by Google
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("title", "Towards Application Portability in Platform as a Service");

        Assert.assertEquals(
                Optional.of(new URL("https://www.uni-bamberg.de/fileadmin/uni/fakultaeten/wiai_lehrstuehle/praktische_informatik/Dateien/Publikationen/sose14-towards-application-portability-in-paas.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void noLinkFound() throws IOException {
        // CI server is blocked by Google
        Assume.assumeFalse(DevEnvironment.isCIServer());

        entry.setField("title", "Pro WF: Windows Workflow in NET 3.5");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}