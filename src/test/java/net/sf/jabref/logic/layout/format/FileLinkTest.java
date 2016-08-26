package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.ParamLayoutFormatter;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class FileLinkTest {

    private FileLinkPreferences prefs;
    @Before
    public void setUp() throws Exception {
        prefs = JabRefPreferences.getInstance().getFileLinkPreferences();
    }

    @Test
    public void testEmpty() {
        assertEquals("", new FileLink(prefs).format(""));
    }

    @Test
    public void testNull() {
        assertEquals("",
                new FileLink(prefs).format(null));
    }

    @Test
    public void testOnlyFilename() {
        assertEquals("test.pdf",
                new FileLink(prefs).format("test.pdf"));
    }

    @Test
    public void testCompleteRecord() {
        assertEquals("test.pdf",
                new FileLink(prefs)
                        .format("paper:test.pdf:PDF"));
    }

    @Test
    public void testMultipleFiles() {
        ParamLayoutFormatter a = new FileLink(prefs);
        assertEquals("test.pdf", a.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

    @Test
    public void testMultipleFilesPick() {
        ParamLayoutFormatter a = new FileLink(prefs);
        a.setArgument("ppt");
        assertEquals("pres.ppt", a.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

    @Test
    public void testMultipleFilesPickNonExistant() {
        ParamLayoutFormatter a = new FileLink(prefs);
        a.setArgument("doc");
        assertEquals("", a.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

}
