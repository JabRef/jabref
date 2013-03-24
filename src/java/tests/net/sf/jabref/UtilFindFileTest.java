package tests.net.sf.jabref;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;

/**
 * Testing Util.findFile for finding files based on regular expressions.
 * 
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class UtilFindFileTest extends FileBasedTestCase {

	String findFile(String dir, String file) {
		return Util.findFile(entry, database, dir, file, true);
	}

	/**
	 * Test that more than one slash is taken to mean that a relative path is to
	 * be returned.
	 * 
	 * @throws IOException
	 */
	public void testFindFileRelative() throws IOException {

		// Most basic case
		assertEqualPaths("HipKro03.pdf", findFile(root.getAbsolutePath() + "/test/",
			"[bibtexkey].pdf"));

		// Including directory
		assertEqualPaths("test/HipKro03.pdf", findFile(root.getAbsolutePath(),
			"test/[bibtexkey].pdf"));

		// No relative paths
		assertEqualPaths(new File(root, "test/HipKro03.pdf").getCanonicalPath(), findFile(null,
			root.getAbsolutePath() + "/test/" + "[bibtexkey].pdf"));

		// No relative paths
		assertEqualPaths(new File(root, "test/HipKro03.pdf").getCanonicalPath(), Util.findFile(
			entry, database, root.getAbsolutePath() + "/test/" + "[bibtexkey].pdf"));

	}
	
	
	

	public void testFindPdf() throws IOException {

		{
			String pdf = Util.findPdf(entry, "pdf", root.getAbsolutePath());
			assertEqualPaths("HipKro03 - Hello.pdf", pdf);

			File fullPath = Util.expandFilename(pdf, root.getAbsolutePath());
			assertTrue(fullPath.exists());
		}
		{
			String pdf = Util.findPdf(entry, "pdf", root.getAbsolutePath() + "/pdfs/");

			assertEqualPaths("sub/HipKro03-sub.pdf", pdf);

			File fullPath = Util.expandFilename(pdf, root.getAbsolutePath() + "/pdfs/");
			assertTrue(fullPath.exists());
		}
	}

	public void testFindAssociatedFiles() throws IOException {
		Collection<BibtexEntry> entries = Arrays.asList(new BibtexEntry[]{entry});
		Collection<String> extensions = Arrays.asList(new String[]{"jpg", "pdf"});
		Collection<File> dirs = Arrays.asList(new File[] { new File(root.getAbsoluteFile() + "/pdfs/"), new File(root.getAbsoluteFile() + "/graphicsDir/") });
		
		Map<BibtexEntry, List<File>> results = Util.findAssociatedFiles(entries, extensions, dirs);
		
		assertEquals(2, results.get(entry).size());
		assertTrue(results.get(entry).contains(new File(root.getAbsoluteFile() + "/graphicsDir/subDir/testHipKro03test.jpg")));
		assertFalse(results.get(entry).contains(new File(root.getAbsoluteFile() + "/graphicsDir/subDir/testHipKro03test.png")));
		assertTrue(results.get(entry).contains(new File(root.getAbsoluteFile() + "/pdfs/sub/HipKro03-sub.pdf")));
	}
	
	public void testFindPdfInMultiple() throws IOException {

		{
			String[] dirsToSearch = new String[] { root.getAbsolutePath(),
				root.getAbsolutePath() + "/pdfs/" };
			String pdf = Util.findPdf(entry, "pdf", dirsToSearch);
			assertEqualPaths("HipKro03 - Hello.pdf", pdf);

			File fullPath = Util.expandFilename(pdf, dirsToSearch);
			assertTrue(fullPath.exists());
			assertEqualPaths(root.getAbsolutePath() + "/HipKro03 - Hello.pdf", fullPath
				.getAbsolutePath());

			String tmp = dirsToSearch[1];
			dirsToSearch[1] = dirsToSearch[0];
			dirsToSearch[0] = tmp;

			fullPath = Util.expandFilename(pdf, dirsToSearch);
			assertTrue(fullPath.exists());
			assertEqualPaths(root.getAbsolutePath() + "/HipKro03 - Hello.pdf", fullPath
				.getAbsolutePath());

			fullPath = Util.expandFilename(pdf, new String[] { dirsToSearch[0] });
			assertEquals(null, fullPath);

			fullPath = Util.expandFilename(pdf, new String[] { dirsToSearch[1] });
			assertTrue(fullPath.exists());
			assertEqualPaths(root.getAbsolutePath() + "/HipKro03 - Hello.pdf", fullPath
				.getAbsolutePath());
		}

		{
			String[] dirsToSearch = new String[] { root.getAbsolutePath() + "/pdfs/",
				root.getAbsolutePath() };
			String pdf = Util.findPdf(entry, "pdf", dirsToSearch);
			assertEqualPaths("sub/HipKro03-sub.pdf", pdf);

			File fullPath = Util.expandFilename(pdf, dirsToSearch);
			assertTrue(fullPath.exists());
			assertEqualPaths(root.getAbsolutePath() + "/pdfs/sub/HipKro03-sub.pdf", fullPath
				.getAbsolutePath());

			String tmp = dirsToSearch[1];
			dirsToSearch[1] = dirsToSearch[0];
			dirsToSearch[0] = tmp;

			fullPath = Util.expandFilename(pdf, dirsToSearch);
			assertTrue(fullPath.exists());
			assertEqualPaths(root.getAbsolutePath() + "/pdfs/sub/HipKro03-sub.pdf", fullPath
				.getAbsolutePath());

			fullPath = Util.expandFilename(pdf, new String[] { dirsToSearch[0] });
			assertEquals(null, fullPath);

			fullPath = Util.expandFilename(pdf, new String[] { dirsToSearch[1] });
			assertTrue(fullPath.exists());
			assertEqualPaths(root.getAbsolutePath() + "/pdfs/sub/HipKro03-sub.pdf", fullPath
				.getAbsolutePath());
		}

	}

	public void testFindFile() throws IOException {

		// Simple case
		assertEqualPaths("HipKro03.pdf", Util.findFile(entry, database, root.getAbsolutePath()
			+ "/test/", "[bibtexkey].pdf", true));

		// Not found
		assertEqualPaths(null, Util.findFile(entry, database, root.getAbsolutePath() + "/test/",
			"Not there [bibtexkey].pdf", true));

		// Test current dir
		assertEqualPaths(new File(new File("."), "build.xml").getCanonicalPath(), Util.findFile(
			entry, database, "./build.xml"));
		assertEqualPaths("build.xml", Util.findFile(entry, database, ".", "build.xml", true));

		// Test keys in path and regular expression in file
		assertEqualPaths(new File(root, "/2003/Paper by HipKro03.pdf").getCanonicalPath(), Util
			.findFile(entry, database, root.getAbsolutePath() + "/[year]/.*[bibtexkey].pdf"));

		// Test . and ..
		assertEqualPaths(new File(root, "/Organization Science/HipKro03 - Hello.pdf")
			.getCanonicalPath(), Util.findFile(entry, database, root.getAbsolutePath()
			+ "/[year]/../2003/.././././[journal]\\" + ".*[bibtexkey].*.pdf"));

		// Test Escape
		assertEqualPaths(new File(root, "/Organization Science/HipKro03 - Hello.pdf")
			.getCanonicalPath(), Util.findFile(entry, database, root.getAbsolutePath() + "/*/"
			+ "[bibtexkey] - Hello\\\\.pdf"));

		assertEqualPaths("TE.ST", Util.findFile(entry, database, root.getAbsolutePath() + "/test/",
			"TE\\\\.ST", true));
		assertEqualPaths(".TEST", Util.findFile(entry, database, root.getAbsolutePath() + "/test/",
			"\\\\.TEST", true));
		assertEqualPaths("TEST[", Util.findFile(entry, database, root.getAbsolutePath() + "/test/",
			"TEST\\\\[", true));

		// Test *
		assertEqualPaths(new File(root, "/Organization Science/HipKro03 - Hello.pdf")
			.getCanonicalPath(), Util.findFile(entry, database, root.getAbsolutePath() + "/*/"
			+ "[bibtexkey].+?.pdf"));

		// Test **
		assertEqualPaths(new File(root, "/pdfs/sub/HipKro03-sub.pdf").getCanonicalPath(), Util
			.findFile(entry, database, root.getAbsolutePath() + "/**/" + "[bibtexkey]-sub.pdf"));

		// Test ** - Find in level itself too
		assertEqualPaths(new File(root, "/pdfs/sub/HipKro03-sub.pdf").getCanonicalPath(), Util
			.findFile(entry, database, root.getAbsolutePath() + "/pdfs/sub/**/"
				+ "[bibtexkey]-sub.pdf"));

		// Test ** - Find lowest level first (Rest is Depth first)
		assertEqualPaths(new File(root, "/HipKro03 - Hello.pdf").getCanonicalPath(), Util.findFile(
			entry, database, root.getAbsolutePath() + "/**/" + "[bibtexkey].*Hello.pdf"));
	}
}
