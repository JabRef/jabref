package org.jabref.model.entry.identifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EprintTest {

    @Test
    public void acceptPlainEprint() {
        assertEquals("0706.0001", new Eprint("0706.0001").getEprint());
    }

    @Test
    public void acceptLegacyEprint() {
        assertEquals("astro-ph.GT/1234567", new Eprint("astro-ph.GT/1234567").getEprint());
        assertEquals("math/1234567", new Eprint("math/1234567").getEprint());
    }

    @Test
    public void acceptPlainEprintWithVersion() {
        assertEquals("0706.0001v1", new Eprint("0706.0001v1").getEprint());
    }

    @Test
    public void ignoreLeadingAndTrailingWhitespaces() {
        assertEquals("0706.0001v1", new Eprint("  0706.0001v1 ").getEprint());
    }

    @Test
    public void rejectEmbeddedEprint() {
        assertThrows(IllegalArgumentException.class, () -> new Eprint("other stuff 0706.0001v1 end"));
    }

    @Test
    public void rejectInvalidEprint() {
        assertThrows(IllegalArgumentException.class, () -> new Eprint("https://thisisnouri"));
    }

    @Test
    public void acceptArxivPrefix() {
        assertEquals("0706.0001v1", new Eprint("arXiv:0706.0001v1").getEprint());
    }

    @Test
    public void acceptURLEprint() {
        // http
        assertEquals("0706.0001v1", new Eprint("http://arxiv.org/abs/0706.0001v1").getEprint());
        // https
        assertEquals("0706.0001v1", new Eprint("https://arxiv.org/abs/0706.0001v1").getEprint());
        // other domains
        assertEquals("0706.0001v1", new Eprint("https://asdf.org/abs/0706.0001v1").getEprint());
    }

    @Test
    public void constructCorrectURLForEprint() {
        assertEquals("http://arxiv.org/abs/0706.0001v1", new Eprint("0706.0001v1").getURIAsASCIIString());
    }
}
