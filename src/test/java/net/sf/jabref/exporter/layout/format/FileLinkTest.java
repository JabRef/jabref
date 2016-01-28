package net.sf.jabref.exporter.layout.format;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;


public class FileLinkTest {

    @Before
    public void setUp() throws Exception {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void testEmpty() {
        assertEquals("", new FileLink().format(""));
    }

    @Test
    public void testNull() {
        assertEquals("", new FileLink().format(null));
    }

    @Test
    public void testOnlyFilename() {
        assertEquals("test.pdf", new FileLink().format("test.pdf"));
    }

    @Test
    public void testCompleteRecord() {
        assertEquals("test.pdf", new FileLink().format("paper:test.pdf:PDF"));
    }

}
