package org.jabref.logic.msbib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MsBibMappingTest {
    @Test
    public void getLanguage() {
        String lang = MSBibMapping.getLanguage(1609);
        assertEquals("basque", lang);
    }

    @Test
    public void getLCID() {
        int lcid = MSBibMapping.getLCID("basque");
        assertEquals(1609, lcid);
    }

    @Test
    public void getInvalidLanguage() {
        String lang = MSBibMapping.getLanguage(1234567);
        assertEquals("english", lang);
    }

    @Test
    public void invalidLCID() {
        int lcid = MSBibMapping.getLCID("not a language");
        assertEquals(1033, lcid);
    }
}
