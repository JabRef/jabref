package org.jabref.logic.importer.fileformat.img;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PngImporterTest {
    private PngImporter importer;

    @BeforeEach
    void setUp() {
        importer = new PngImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("PNG", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("png", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.PNG, importer.getFileType());
    }
}
