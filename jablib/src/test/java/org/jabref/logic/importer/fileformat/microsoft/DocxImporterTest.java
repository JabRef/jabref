package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.importer.fileformat.odf.OdpImporter;
import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordImporterTest {
    private OdpImporter importer;

    @BeforeEach
    void setUp() {
        importer = new OdpImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("OpenDocument Impress", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("odp", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.ODP, importer.getFileType());
    }
}
