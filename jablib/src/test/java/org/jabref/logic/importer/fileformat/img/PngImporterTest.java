package org.jabref.logic.importer.fileformat.img;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PngImporterTest {
    private JpgImporter importer;

    @BeforeEach
    void setUp() {
        importer = new JpgImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("JPG", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("jpg", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.JPG, importer.getFileType());
    }
}
