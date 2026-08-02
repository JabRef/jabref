package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import javax.xml.stream.XMLInputFactory;

import org.jabref.logic.importer.ImportException;
import org.jabref.logic.util.StandardFileType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Articles in the medline format can be downloaded from http://www.ncbi.nlm.nih.gov/pubmed/.
/// <ol>
/// - Search for a term and make sure you have selected the **PubMed** database.
/// - Select the results you want to export by checking their checkboxes.
/// - Press on the **'Send to'** drop down menu on top of the search results.
/// - Select **'File'** as Destination and **'XML'** as Format.
/// - Press **'Create File'** to download your search results in a medline xml file.
/// </ol>
class MedlineImporterTest {

    private static final String AALTO_INPUT_FACTORY = "com.fasterxml.aalto.stax.InputFactoryImpl";

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

    @Test
    void meshHeadingListIsParsedIntoIndividualKeywords() throws IOException, ImportException {
        ImporterTestEngine.testImportEntries(importer, "MedlineImporterTestMeshHeadingList.xml", ".xml");
    }

    @Test
    void rejectsExternalEntities() throws IOException {
        String xmlWithExternalEntity = """
                <!DOCTYPE PubmedArticleSet [<!ENTITY entity SYSTEM "file:///not-accessed">]>
                <PubmedArticleSet><PubmedArticle><PMID>&entity;</PMID></PubmedArticle></PubmedArticleSet>
                """;

        assertTrue(importer.importDatabase(new BufferedReader(Reader.of(xmlWithExternalEntity))).isInvalid());
    }

    @Test
    void xmlInputFactoryUsesAaltoAndSupportsSecureProperties() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        assertEquals(AALTO_INPUT_FACTORY, xmlInputFactory.getClass().getName());
        assertEquals(false, xmlInputFactory.getProperty(XMLInputFactory.SUPPORT_DTD));
        assertEquals(false, xmlInputFactory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));
    }
}
