package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PptImporterTest {
    private PptImporter importer;

    @BeforeEach
    void setUp() {
        importer = new PptImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Microsoft PowerPoint 97-2003", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("ppt", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.PPT, importer.getFileType());
    }
}
