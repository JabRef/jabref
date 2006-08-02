package tests.net.sf.jabref;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

/**
 * Testing Util.findFile for finding files based on regular expressions.
 * 
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class UtilFindFileTest extends TestCase {

	public void testFindFile() throws IOException {

		assertEqualPaths(
			new File(root, "/test/HipKro03.pdf").getCanonicalPath(), 
			Util.findFile(entry, database, root.getAbsolutePath() + "/test/", "[bibtexkey].pdf"));

		// Test keys in path and regular expression in file
		assertEqualPaths(
			new File(new File("."), "build.xml").getCanonicalPath(),
			Util.findFile(entry, database, "", "build.xml"));

		// Test keys in path and regular expression in file
		assertEqualPaths(
			new File(root, "/2003/Paper by HipKro03.pdf").getCanonicalPath(),
			Util.findFile(entry, database, root.getAbsolutePath() + "/[year]/", ".*[bibtexkey].pdf"));

		// Test . and ..
		assertEqualPaths(
			new File(root, "/Organization Science/HipKro03 - Hello.pdf").getCanonicalPath(),
			Util.findFile(entry, database, root.getAbsolutePath()
				+ "/[year]/../2003/.././././[journal]\\", ".*[bibtexkey].*.pdf"));

		// Test *
		assertEqualPaths(new File(root, "/Organization Science/HipKro03 - Hello.pdf").getCanonicalPath(),
			Util.findFile(entry, database, root.getAbsolutePath() + "/*/", "[bibtexkey].+\\.pdf"));

		// Test **
		assertEqualPaths(new File(root, "/pdfs/sub/HipKro03-sub.pdf").getCanonicalPath(),
			Util.findFile(entry, database, root.getAbsolutePath() + "/**", "[bibtexkey]-sub.pdf"));
		
		// Test ** - Find in level itself too
		assertEqualPaths(new File(root, "/pdfs/sub/HipKro03-sub.pdf").getCanonicalPath(),
			Util.findFile(entry, database, root.getAbsolutePath() + "/pdfs/sub/**", "[bibtexkey]-sub.pdf"));

		// Test ** - Find lowest level first (Rest is Depth first)
		assertEqualPaths(new File(root, "/HipKro03 - Hello.pdf").getCanonicalPath(),
			Util.findFile(entry, database, root.getAbsolutePath() + "/**", "[bibtexkey].*Hello.pdf"));
	}

	BibtexDatabase database;

	BibtexEntry entry;

	File root;

	public void setUp() {

		StringReader reader = new StringReader(
			"@ARTICLE{HipKro03,\n"
				+ "  author = {Eric von Hippel and Georg von Krogh},\n"
				+ "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},\n"
				+ "  journal = {Organization Science},\n"
				+ "  year = {2003},\n"
				+ "  volume = {14},\n"
				+ "  pages = {209--223},\n"
				+ "  number = {2},\n"
				+ "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},\n" + "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n"
				+ "  issn = {1526-5455}," + "\n" + "  publisher = {INFORMS}\n" 
				+ "}");

		BibtexParser parser = new BibtexParser(reader);
		ParserResult result = null;
		try {
			result = parser.parse();
		} catch (Exception e) {
			fail();
		}
		database = result.getDatabase();
		entry = database.getEntriesByKey("HipKro03")[0];

		assertNotNull(database);
		assertNotNull(entry);

		// Create file structure
		try {
			root = createTempDir("UtilFindFileTest");

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

			File foo = new File(dirTest, "foo.dat");
			foo.createNewFile();

		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public void tearDown() {
		deleteRecursive(root);
	}
	
	/**
	 * Will check if two paths are the same.
	 */
	public static void assertEqualPaths(String path1, String path2) {
		// System.out.println(path1);
		if ((path1 == null || path2 == null) && path1 != path2)
			fail("Path1 was: " + path1 + " but Path2 was: " + path2);

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

}
