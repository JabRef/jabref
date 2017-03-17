package org.jabref.model.entry.identifier;

import org.junit.Assert;
import org.junit.Test;

public class EprintTest {

    @Test
    public void acceptPlainEprint() {
        Assert.assertEquals("0706.0001", new Eprint("0706.0001").getEprint());
    }

    @Test
    public void acceptLegacyEprint() {
        Assert.assertEquals("astro-ph.GT/1234567", new Eprint("astro-ph.GT/1234567").getEprint());
        Assert.assertEquals("math/1234567", new Eprint("math/1234567").getEprint());
    }

    @Test
    public void acceptPlainEprintWithVersion() {
        Assert.assertEquals("0706.0001v1", new Eprint("0706.0001v1").getEprint());
    }

    @Test
    public void ignoreLeadingAndTrailingWhitespaces() {
        Assert.assertEquals("0706.0001v1", new Eprint("  0706.0001v1 ").getEprint());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectEmbeddedEprint() {
        new Eprint("other stuff 0706.0001v1 end");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectInvalidEprint() {
        new Eprint("https://thisisnouri");
    }

    @Test
    public void acceptArxivPrefix() {
        Assert.assertEquals("0706.0001v1", new Eprint("arXiv:0706.0001v1").getEprint());
    }

    @Test
    public void acceptURLEprint() {
        // http
        Assert.assertEquals("0706.0001v1", new Eprint("http://arxiv.org/abs/0706.0001v1").getEprint());
        // https
        Assert.assertEquals("0706.0001v1", new Eprint("https://arxiv.org/abs/0706.0001v1").getEprint());
        // other domains
        Assert.assertEquals("0706.0001v1", new Eprint("https://asdf.org/abs/0706.0001v1").getEprint());
    }

    @Test
    public void constructCorrectURLForEprint() {
        Assert.assertEquals("http://arxiv.org/abs/0706.0001v1", new Eprint("0706.0001v1").getURIAsASCIIString());
    }
}
