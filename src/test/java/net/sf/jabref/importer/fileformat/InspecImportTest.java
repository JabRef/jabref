package net.sf.jabref.importer.fileformat;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

/**
 *
 * This is the test class of the InspecImporter. It tests all 4 methods of the class.
 *
 * @author mairdl
 * @author braunch
 *
 */
public class InspecImportTest {

    InspecImporter inspecImp;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
        this.inspecImp = new InspecImporter();

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link net.sf.jabref.importer.fileformat.InspecImporter#isRecognizedFormat(java.io.InputStream)}.
     * 
     * @throws IOException
     */
    @Test
    public void testIsRecognizedFormat() throws IOException {

        InputStream testInput = InspecImportTest.class.getResourceAsStream("InpsecImportTest.txt");

        Assert.assertTrue("InspecImporter isRecognizedFormat test failed", inspecImp.isRecognizedFormat(testInput));

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

        assertEquals("testGetFormatName failed", "INSPEC", inspecImp.getFormatName());
    }

    /**
     * Test method for {@link net.sf.jabref.importer.fileformat.InspecImporter#getCLIId()}.
     */
    @Test
    public void testGetCLIId() {

        assertEquals("testGetCLIId failed", "inspec", inspecImp.getCLIId());

    }

}
