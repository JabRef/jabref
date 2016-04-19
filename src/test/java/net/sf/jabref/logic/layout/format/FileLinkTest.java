package net.sf.jabref.logic.layout.format;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


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

    @Test
    public void testMultipleFiles() {
        ParamLayoutFormatter a = new FileLink();
        assertEquals("test.pdf", a.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

    @Test
    public void testMultipleFilesPick() {
        ParamLayoutFormatter a = new FileLink();
        a.setArgument("ppt");
        assertEquals("pres.ppt", a.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

    @Test
    public void testMultipleFilesPickNonExistant() {
        ParamLayoutFormatter a = new FileLink();
        a.setArgument("doc");
        assertEquals("", a.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

}
