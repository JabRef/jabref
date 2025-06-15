package org.jabref.logic.importer.fileformat;

import org.jabref.logic.importer.fileformat.microsoft.XlsImporter;
import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxtImporterTest {
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
