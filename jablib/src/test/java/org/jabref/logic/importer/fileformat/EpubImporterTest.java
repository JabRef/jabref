package org.jabref.logic.importer.fileformat;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class EpubImporterTest {
    private EpubImporter importer;

    @BeforeEach
    void setUp() throws XPathExpressionException, ParserConfigurationException {
        this.importer = new EpubImporter(mock(ImportFormatPreferences.class));
    }

    @Test
    void getFormatName() {
        assertEquals("ePUB", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("epub", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.EPUB, importer.getFileType());
    }
}
