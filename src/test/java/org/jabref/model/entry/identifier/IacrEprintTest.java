package org.jabref.model.entry.identifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IacrEprintTest {

    @Test
    public void acceptPlainIacrEprint() {
        assertEquals("2019/001", new IacrEprint("2019/001").getNormalized());
    }

    @Test
    public void ignoreLeadingAndTrailingWhitespaces() {
        assertEquals("2019/001", new IacrEprint(" 2019/001   ").getNormalized());
    }

    @Test
    public void rejectInvalidIacrEprint() {
        assertThrows(IllegalArgumentException.class,() -> new IacrEprint(" 2021/12"));
    }

    @Test
    public void acceptFullUrlIacrEprint() {
        assertEquals("2019/001", new IacrEprint("https://eprint.iacr.org/2019/001").getNormalized());
    }

    @Test
    public void acceptShortenedUrlIacrEprint() {
        assertEquals("2019/001", new IacrEprint("https://ia.cr/2019/001").getNormalized());
    }

    @Test
    public void acceptDomainUrlIacrEprint() {
        assertEquals("2019/001", new IacrEprint("eprint.iacr.org/2019/001").getNormalized());
    }

    @Test
    public void acceptShortenedDomainUrlIacrEprint() {
        assertEquals("2019/001", new IacrEprint("ia.cr/2019/001").getNormalized());
    }


    @Test
    public void constructValidIacrEprintUrl() {
        assertEquals("https://ia.cr/2019/001", new IacrEprint("2019/001").getAsciiUrl());
    }

}
