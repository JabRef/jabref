package tests.net.sf.jabref;

import java.io.File;
import java.io.StringReader;

import junit.framework.TestCase;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

/**
 * A base class for Testing in JabRef that comes along with some useful
 * functions.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class FileBasedTestCase extends TestCase {

	/**
	 * Will check if two paths are the same.
	 */
	public static void assertEqualPaths(String path1, String path2) {

		if (path1 == path2)
			return;

		if ((path1 == null || path2 == null) && path1 != path2)
			fail("Expected: " + path1 + " but was: " + path2);

		assertEquals(path1.replaceAll("\\\\", "/"), path2.replaceAll("\\\\", "/"));
	}

	/**
	 * Creates a temp directory in the System temp directory.
	 * 
	 * Taken from
	 * http://forum.java.sun.com/thread.jspa?threadID=470197&messageID=2169110
	 * 
	 * Author: jfbriere
	 * 
	 * @return returns null if directory could not created.
	 */
	public static File createTempDir(String prefix) {
		return createTempDir(prefix, null);
	}

	/**
	 * Creates a temp directory in a given directory.
	 * 
	 * Taken from
	 * http://forum.java.sun.com/thread.jspa?threadID=470197&messageID=2169110
	 * 
	 * Author: jfbriere
	 * 
	 * @param directory
	 *            MayBeNull - null indicates that the system tmp directory
	 *            should be used.
	 * 
	 * @return returns null if directory could not created.
	 */
	public static File createTempDir(String prefix, File directory) {
		try {
			File tempFile = File.createTempFile(prefix, "", directory);

			if (!tempFile.delete())
				return null;
			if (!tempFile.mkdir())
				return null;

			return tempFile;

		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Deletes a directory or file
	 * 
	 * Taken from
	 * http://forum.java.sun.com/thread.jspa?threadID=470197&messageID=2169110
	 * 
	 * Author: jfbriere
	 * 
	 * @param file
	 */
	public static void deleteRecursive(File file) {
		if (file.isDirectory()) {
			File[] fileArray = file.listFiles();

			if (fileArray != null)
				for (int i = 0; i < fileArray.length; i++)
					deleteRecursive(fileArray[i]);
		}
		file.delete();
	}

	static BibtexDatabase database;

	static BibtexEntry entry;

	File root;

	private String oldPdfDirectory;

	private boolean oldUseRegExp;

	public static BibtexEntry getBibtexEntry() {

		if (database == null) {

			StringReader reader = new StringReader(
				"@ARTICLE{HipKro03,\n"
					+ "  author = {Eric von Hippel and Georg von Krogh},\n"
					+ "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},\n"
					+ "  journal = {Organization Science},\n"
					+ "  year = {2003},\n"
					+ "  volume = {14},\n"
					+ "  pages = {209--223},\n"
					+ "  number = {2},\n"
					+ "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},\n"
					+ "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n"
					+ "  issn = {1526-5455}," + "\n" + "  publisher = {INFORMS}\n" + "}");

			BibtexParser parser = new BibtexParser(reader);
			ParserResult result = null;
			try {
				result = parser.parse();
			} catch (Exception e) {
				fail();
			}
			database = result.getDatabase();
			entry = database.getEntriesByKey("HipKro03")[0];
		}
		return entry;
	}

	public void setUp() throws Exception {

		Globals.prefs = JabRefPreferences.getInstance();
		oldUseRegExp = Globals.prefs.getBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY);
		oldPdfDirectory = Globals.prefs.get("pdfDirectory");

		Globals.prefs.putBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY, false);
		
		getBibtexEntry();
		assertNotNull(database);
		assertNotNull(entry);

		// Create file structure
		try {
			root = createTempDir("UtilFindFileTest");

			Globals.prefs.put("pdfDirectory", root.getPath());
		
			File subDir1 = new File(root, "Organization Science");
			subDir1.mkdir();

			File pdf1 = new File(subDir1, "HipKro03 - Hello.pdf");
			pdf1.createNewFile();

			File pdf1a = new File(root, "HipKro03 - Hello.pdf");
			pdf1a.createNewFile();

			File subDir2 = new File(root, "pdfs");
			subDir2.mkdir();

			File subsubDir1 = new File(subDir2, "sub");
			subsubDir1.mkdir();

			File pdf2 = new File(subsubDir1, "HipKro03-sub.pdf");
			pdf2.createNewFile();

			File dir2002 = new File(root, "2002");
			dir2002.mkdir();

			File dir2003 = new File(root, "2003");
			dir2003.mkdir();

			File pdf3 = new File(dir2003, "Paper by HipKro03.pdf");
			pdf3.createNewFile();

			File dirTest = new File(root, "test");
			dirTest.mkdir();

			File pdf4 = new File(dirTest, "HipKro03.pdf");
			pdf4.createNewFile();

			File pdf5 = new File(dirTest, ".TEST");
			pdf5.createNewFile();

			File pdf6 = new File(dirTest, "TEST[");
			pdf6.createNewFile();

			File pdf7 = new File(dirTest, "TE.ST");
			pdf7.createNewFile();

			File foo = new File(dirTest, "foo.dat");
			foo.createNewFile();
			
			File graphicsDir = new File(root, "graphicsDir");
			graphicsDir.mkdir();
			
			File graphicsSubDir = new File(graphicsDir, "subDir");
			graphicsSubDir.mkdir();
			
			File jpg = new File(graphicsSubDir, "testHipKro03test.jpg");
			jpg.createNewFile();

			File png = new File(graphicsSubDir, "testHipKro03test.png");
			png.createNewFile();
			
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public void tearDown() {
		deleteRecursive(root);
		Globals.prefs.putBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY, oldUseRegExp);
		Globals.prefs.put("pdfDirectory", oldPdfDirectory);
		// TODO: This is not a great way to do this, sure ;-)
	}
	
	public void testVoid(){
		// to remove warning
	}
	
}
