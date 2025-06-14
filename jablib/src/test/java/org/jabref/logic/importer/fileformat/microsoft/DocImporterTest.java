package org.jabref.logic.importer.fileformat.microsoft;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordOldImporterTest {
    private WordOldImporter importer;

    @BeforeEach
    void setUp() {
        importer = new WordOldImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Microsoft Word 97-2003", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("doc", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.DOC, importer.getFileType());
    }
}
