package net.sf.jabref.logic.fulltext;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ArXivTest {

    private ArXiv finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        finder = new ArXiv();
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
        entry.setField("doi", "10.1529/biophysj.104.047340");

        Assert.assertEquals(
                Optional.of(new URL("http://arxiv.org/pdf/cond-mat/0406246v1")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void findByEprint() throws IOException {
        entry.setField("eprint", "1603.06570");

        Assert.assertEquals(
                Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void findByEprintWithUnknownDOI() throws IOException {
        entry.setField("doi", "10.1529/unknown");
        entry.setField("eprint", "1603.06570");

        Assert.assertEquals(
                Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void notFoundByUnknownDOI() throws IOException {
        entry.setField("doi", "10.1529/unknown");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void notFoundByMalformedId() throws IOException {
        entry.setField("eprint", "1234.12345");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    /*
    @Test
    public void searchWithMalformedIdThrowsException() throws IOException {
        entry.setField("eprint", "1234.12345");

        finder


        Assert.assertEquals(
                Optional.of(new URL("http://arxiv.org/pdf/1603.06570v1")),
                finder.findFullText(entry)
        );
    }
    */
}
