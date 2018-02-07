package org.jabref.logic.importer.fileformat;

import org.jabref.logic.util.FileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class BibTeXMLImporterTest {

    private BibTeXMLImporter importer;

    @BeforeEach
    public void setUp() throws Exception {
        importer = new BibTeXMLImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("BibTeXML", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("bibtexml", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.BIBTEXML, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the BibTeXML format.", importer.getDescription());
    }
}
