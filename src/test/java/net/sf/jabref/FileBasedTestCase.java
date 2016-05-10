package net.sf.jabref;

import java.io.File;
import java.io.IOException;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * A base class for Testing in JabRef that comes along with some useful
 * functions.
 */
public class FileBasedTestCase {

    protected BibDatabase database;
    protected BibEntry entry;
    protected File root;

    private String oldPdfDirectory;
    private boolean oldUseRegExp;
    private boolean oldAutoLinkExcatKeyOnly;


    @Before
    public void setUp() throws IOException {
        Globals.prefs = JabRefPreferences.getInstance();
        oldUseRegExp = Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY);
        oldAutoLinkExcatKeyOnly = Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY);
        oldPdfDirectory = Globals.prefs.get("pdfDirectory");

        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, false);
        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, false);

        database = BibtexTestData.getBibtexDatabase();
        entry = database.getEntries().iterator().next();

        root = FileBasedTestHelper.createTempDir("UtilFindFileTest");

        Assert.assertNotNull(root);

        Globals.prefs.put("pdfDirectory", root.getPath());

        File subDir1 = new File(root, "Organization Science");
        Assert.assertTrue(subDir1.mkdir());

        File pdf1 = new File(subDir1, "HipKro03 - Hello.pdf");
        Assert.assertTrue(pdf1.createNewFile());

        File pdf1a = new File(root, "HipKro03 - Hello.pdf");
        Assert.assertTrue(pdf1a.createNewFile());

        File subDir2 = new File(root, "pdfs");
        Assert.assertTrue(subDir2.mkdir());

        File subsubDir1 = new File(subDir2, "sub");
        Assert.assertTrue(subsubDir1.mkdir());

        File pdf2 = new File(subsubDir1, "HipKro03-sub.pdf");
        Assert.assertTrue(pdf2.createNewFile());

        File dir2002 = new File(root, "2002");
        Assert.assertTrue(dir2002.mkdir());

        File dir2003 = new File(root, "2003");
        Assert.assertTrue(dir2003.mkdir());

        File pdf3 = new File(dir2003, "Paper by HipKro03.pdf");
        Assert.assertTrue(pdf3.createNewFile());

        File dirTest = new File(root, "test");
        Assert.assertTrue(dirTest.mkdir());

        File pdf4 = new File(dirTest, "HipKro03.pdf");
        Assert.assertTrue(pdf4.createNewFile());

        File pdf5 = new File(dirTest, ".TEST");
        Assert.assertTrue(pdf5.createNewFile());

        File pdf6 = new File(dirTest, "TEST[");
        Assert.assertTrue(pdf6.createNewFile());

        File pdf7 = new File(dirTest, "TE.ST");
        Assert.assertTrue(pdf7.createNewFile());

        File foo = new File(dirTest, "foo.dat");
        Assert.assertTrue(foo.createNewFile());

        File graphicsDir = new File(root, "graphicsDir");
        Assert.assertTrue(graphicsDir.mkdir());

        File graphicsSubDir = new File(graphicsDir, "subDir");
        Assert.assertTrue(graphicsSubDir.mkdir());

        File jpg = new File(graphicsSubDir, "HipKro03test.jpg");
        Assert.assertTrue(jpg.createNewFile());

        File png = new File(graphicsSubDir, "HipKro03test.png");
        Assert.assertTrue(png.createNewFile());

    }

    @After
    public void tearDown() {
        FileBasedTestHelper.deleteRecursive(root);
        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, oldAutoLinkExcatKeyOnly);
        Globals.prefs.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, oldUseRegExp);
        Globals.prefs.put("pdfDirectory", oldPdfDirectory);
        // TODO: This is not a great way to do this, sure ;-)
    }

}
