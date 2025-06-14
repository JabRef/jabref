package org.jabref.logic.importer.fileformat.odf;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OdsImporterTest {
    private OdsImporter importer;

    @BeforeEach
    void setUp() {
        importer = new OdsImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("OpenDocument Calc", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("ods", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.ODS, importer.getFileType());
    }
}
