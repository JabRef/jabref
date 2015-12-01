package net.sf.jabref.importer.fileformat;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.importer.fileformat.BiblioscapeImporter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/*
 * Biblioscape Tag File with an example:
 * https://web.archive.org/web/20080820010408/http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm
 */
public class BiblioscapeImporterTest {

    @Before
    public void setUp() throws Exception {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void testGetFormatName() {
        BiblioscapeImporter importer = new BiblioscapeImporter();
        Assert.assertEquals(importer.getFormatName(), "Biblioscape");
    }

    @Test
    public void testGetCLIID() {
        BiblioscapeImporter importer = new BiblioscapeImporter();
        Assert.assertEquals(importer.getCLIId(), "biblioscape");
    }

    @Test
    public void testIsRecognizedFormat() throws Throwable {
        BiblioscapeImporter importer = new BiblioscapeImporter();
        LinkedList<String> list = new LinkedList<String>();
        list.add("BiblioscapeImporterTest.txt");
        list.add("IEEEImport1.txt");
        list.add("IsiImporterTest1.isi");
        list.add("IsiImporterTestInspec.isi");
        list.add("IsiImporterTestWOS.isi");
        list.add("IsiImporterTestMedline.isi");
        list.add("RisImporterTest1.ris");
        for (String str : list) {
            Throwable throwable = null;
            Object var6_7 = null;
            try {
                InputStream is = BiblioscapeImporterTest.class.getResourceAsStream(str);
                try {
                    Assert.assertTrue(importer.isRecognizedFormat(is));
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (Throwable var6_8) {
                if (throwable == null) {
                    throwable = var6_8;
                } else if (throwable != var6_8) {
                    throwable.addSuppressed(var6_8);
                }
                throw throwable;
            }
        }
    }

    @Test
    public void testImportEntries1() throws Throwable {
        BiblioscapeImporter importer = new BiblioscapeImporter();
        Throwable throwable = null;
        Object var3_4 = null;
        try {
            InputStream is = BiblioscapeImporter.class.getResourceAsStream("BiblioscapeImporterTest.txt");
            try {
                List entries = importer.importEntries(is, new OutputPrinterToNull());
                Assert.assertEquals(7, entries.size());
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (Throwable var3_5) {
            if (throwable == null) {
                throwable = var3_5;
            } else if (throwable != var3_5) {
                throwable.addSuppressed(var3_5);
            }
            throw throwable;
        }
    }
}
