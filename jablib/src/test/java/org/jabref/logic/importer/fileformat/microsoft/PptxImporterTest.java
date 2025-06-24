package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PptxImporterTest {
    private PptxImporter importer;

    @BeforeEach
    void setUp() {
        importer = new PptxImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Microsoft PowerPoint 2007-365", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("pptx", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.PPTX, importer.getFileType());
    }
}
