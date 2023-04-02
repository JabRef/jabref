package org.jabref.logic.msbib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MsBibMappingTest {
    @Test
    public void testGetLanguage() {
        String lang = MSBibMapping.getLanguage(1609);
        assertEquals("basque", lang);
    }

    @Test
    public void testGetLCID() {
        int lcid = MSBibMapping.getLCID("basque");
        assertEquals(1609, lcid);
    }
}
