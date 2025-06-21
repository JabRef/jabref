package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XlsImporterTest {
    private XlsImporter importer;

    @BeforeEach
    void setUp() {
        importer = new XlsImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Microsoft Excel 97-2003", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("xls", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.XLS, importer.getFileType());
    }
}
