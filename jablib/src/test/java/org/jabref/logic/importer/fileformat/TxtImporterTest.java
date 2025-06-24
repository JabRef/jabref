package org.jabref.logic.importer.fileformat;

import org.jabref.logic.importer.fileformat.misc.TxtImporter;
import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxtImporterTest {
    private TxtImporter importer;

    @BeforeEach
    void setUp() {
        importer = new TxtImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("TXT", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("txt", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.TXT, importer.getFileType());
    }
}
