package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XlsxImporterTest {
    private XlsxImporter importer;

    @BeforeEach
    void setUp() {
        importer = new XlsxImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Microsoft Excel 2007-365", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("xlsx", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.XLSX, importer.getFileType());
    }
}
