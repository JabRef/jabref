package org.jabref.logic.importer.fileformat;

import org.jabref.logic.util.FileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Articles in the medline format can be downloaded from http://www.ncbi.nlm.nih.gov/pubmed/.
 * 1. Search for a term and make sure you have selected the PubMed database
 * 2. Select the results you want to export by checking their checkboxes
 * 3. Press on the 'Send to' drop down menu on top of the search results
 * 4. Select 'File' as Destination and 'XML' as Format
 * 5. Press 'Create File' to download your search results in a medline xml file
 *
 */
public class MedlineImporterTest {

    private MedlineImporter importer;

    @BeforeEach
    public void setUp() throws Exception {
        this.importer = new MedlineImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Medline/PubMed", importer.getName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("medline", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileType.MEDLINE, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the Medline format.", importer.getDescription());
    }

}
