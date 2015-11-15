package net.sf.jabref.importer.fileformat;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * This is the test class of the InspecImporter. It test all 4 methodes of the class.
 *
 * @author mairdl
 * @author braunch
 *
 */
public class InspecImportTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link net.sf.jabref.importer.fileformat.InspecImporter#isRecognizedFormat(java.io.InputStream)}.
     */
    @Test
    public void testIsRecognizedFormat() {
        //TODO complete the test
        // InspecImporter inspecImp = new InspecImporter();
        // instream ...
        // assertTrue("testIsRecognizedFormat failed",inspecImp.isRecognizedFormat(inStream));
        fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link net.sf.jabref.importer.fileformat.InspecImporter#importEntries(java.io.InputStream, net.sf.jabref.importer.OutputPrinter)}
     * .
     */
    @Test
    public void testImportEntries() {
        //TODO complete the test
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link net.sf.jabref.importer.fileformat.InspecImporter#getFormatName()}.
     */
    @Test
    public void testGetFormatName() {
        InspecImporter inspecImp = new InspecImporter();
        assertEquals("testGetFormatName failed", "INSPEC", inspecImp.getFormatName());
    }

    /**
     * Test method for {@link net.sf.jabref.importer.fileformat.InspecImporter#getCLIId()}.
     */
    @Test
    public void testGetCLIId() {
        InspecImporter inspecImp = new InspecImporter();
        assertEquals("testGetCLIId failed", "inspec", inspecImp.getCLIId());

    }

}
