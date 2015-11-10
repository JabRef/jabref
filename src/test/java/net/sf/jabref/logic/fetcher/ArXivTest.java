package net.sf.jabref.logic.fetcher;

import net.sf.jabref.model.entry.BibtexEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class ArXivTest {
    ArXiv finder;
    BibtexEntry entry;

    @Before
    public void setup() {
        finder = new ArXiv();
        entry = new BibtexEntry();
    }

    @Test(expected = NullPointerException.class)
    public void rejectNullParameter() throws IOException {
        finder.findFullText(null);
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
    public void notFoundByDOI() throws IOException {
        entry.setField("doi", "10.1529/unknown");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }
}