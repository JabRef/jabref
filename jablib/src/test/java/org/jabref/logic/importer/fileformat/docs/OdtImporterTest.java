package org.jabref.logic.importer.fileformat.docs;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OdtImporterTest {
    private OdtImporter importer;

    @BeforeEach
    void setUp() {
        importer = new OdtImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("OpenDocument Writer", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("odt", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.ODT, importer.getFileType());
    }
}
