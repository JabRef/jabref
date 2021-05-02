package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class FileLinkTest {

    private FileLinkPreferences prefs;
    private ParamLayoutFormatter fileLinkLayoutFormatter;

    @BeforeEach
    public void setUp() throws Exception {
        prefs = mock(FileLinkPreferences.class);
        fileLinkLayoutFormatter = new FileLink(prefs);
    }

    @Test
    public void formatEmpty() {
        assertEquals("", fileLinkLayoutFormatter.format(""));
    }

    @Test
    public void formatNull() {
        assertEquals("", fileLinkLayoutFormatter.format(null));
    }

    @Test
    public void formatOnlyFilename() {
        assertEquals("test.pdf", fileLinkLayoutFormatter.format("test.pdf"));
    }

    @Test
    public void formatCompleteRecord() {
        assertEquals("test.pdf", fileLinkLayoutFormatter.format("paper:test.pdf:PDF"));
    }

    @Test
    public void formatMultipleFiles() {
        assertEquals("test.pdf", fileLinkLayoutFormatter.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

    @Test
    public void formatMultipleFilesPick() {
        fileLinkLayoutFormatter.setArgument("ppt");
        assertEquals("pres.ppt", fileLinkLayoutFormatter.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }

    @Test
    public void formatMultipleFilesPickNonExistant() {
        fileLinkLayoutFormatter.setArgument("doc");
        assertEquals("", fileLinkLayoutFormatter.format("paper:test.pdf:PDF;presentation:pres.ppt:PPT"));
    }
}
