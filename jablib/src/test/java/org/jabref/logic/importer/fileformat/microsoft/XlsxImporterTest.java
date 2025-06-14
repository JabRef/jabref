package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XlsxImporterTest {
    private DocxImporter importer;

    @BeforeEach
    void setUp() {
        importer = new DocxImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Microsoft Word 2007-365", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("docx", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.DOCX, importer.getFileType());
    }
}
