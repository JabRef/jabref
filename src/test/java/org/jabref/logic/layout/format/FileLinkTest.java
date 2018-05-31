package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;


public class FileLinkTest {

    private FileLinkPreferences prefs;
    @BeforeEach
    public void setUp() throws Exception {
        prefs = mock(FileLinkPreferences.class);
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
