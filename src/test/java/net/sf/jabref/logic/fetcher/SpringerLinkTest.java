package net.sf.jabref.logic.fetcher;

import net.sf.jabref.model.entry.BibEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class SpringerLinkTest {
    SpringerLink finder;
    BibEntry entry;

    @Before
    public void setup() {
        finder = new SpringerLink();
        entry = new BibEntry();
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
        entry.setField("doi", "10.1186/s13677-015-0042-8");

        Assert.assertEquals(
                Optional.of(new URL("http://link.springer.com/content/pdf/10.1186/s13677-015-0042-8.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1186/unknown-doi");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}
