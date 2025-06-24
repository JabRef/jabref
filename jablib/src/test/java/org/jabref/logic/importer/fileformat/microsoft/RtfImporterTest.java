package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RtfImporterTest {
    private RtfImporter importer;

    @BeforeEach
    void setUp() {
        importer = new RtfImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("RTF", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("rtf", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.RTF, importer.getFileType());
    }
}
