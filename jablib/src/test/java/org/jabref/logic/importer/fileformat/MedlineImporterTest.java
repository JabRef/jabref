package org.jabref.logic.importer.fileformat;

import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Articles in the medline format can be downloaded from http://www.ncbi.nlm.nih.gov/pubmed/.
/// <ol>
/// - Search for a term and make sure you have selected the **PubMed** database.
/// - Select the results you want to export by checking their checkboxes.
/// - Press on the **'Send to'** drop down menu on top of the search results.
/// - Select **'File'** as Destination and **'XML'** as Format.
/// - Press **'Create File'** to download your search results in a medline xml file.
/// </ol>
class MedlineImporterTest {

    private MedlineImporter importer;

    @BeforeEach
    void setUp() {
        this.importer = new MedlineImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Medline/PubMed", importer.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("medline", importer.getId());
    }

    @Test
    void sGetExtensions() {
        assertEquals(StandardFileType.MEDLINE, importer.getFileType());
    }
}
