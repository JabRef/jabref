package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.imports.IsiImporter;

/**
 * Test cases for the IsiImporter
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public class IsiImporterTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();

		if (Globals.prefs == null) {
			Globals.prefs = JabRefPreferences.getInstance();
		}

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIsRecognizedFormat() throws IOException {
		IsiImporter importer = new IsiImporter();
		assertTrue(importer.isRecognizedFormat(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTest1.isi")));
		
		// Current detection works based on years. This is scetchy.
		fail();
	}

	public void testImportEntries() throws IOException {
		IsiImporter importer = new IsiImporter();

		List entries = importer.importEntries(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTest1.isi"));// new
															// FileInputStream("tests/net/sf/jabref/imports/IsiImporterTest1.isi"));//new
															// ByteArrayInputStream(test1.getBytes()));

		assertEquals(1, entries.size());
		BibtexEntry entry = (BibtexEntry) entries.get(0);
		assertEquals("Optical properties of MgO doped LiNbO/sub 3/ single crystals", entry
			.getField("title"));
		assertEquals(
			"James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J.",
			entry.getField("author"));

		assertEquals(BibtexEntryType.ARTICLE, entry.getType());
		assertEquals("Optical Materials", entry.getField("journal"));
		assertEquals("2006", entry.getField("year"));
		assertEquals("28", entry.getField("volume"));
		assertEquals("5", entry.getField("number"));
		assertEquals("467--472", entry.getField("pages"));

		// What todo with PD and UT?
	}

	public void testIsiAuthorsConvert() {
		assertEquals(
			"James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J.",
			IsiImporter
				.isiAuthorsConvert("James Brown and James Marc Brown and Brown, J.M. and Brown, J. and Brown, J.M. and Brown, J."));
	}

	public void testIsiAuthorConvert() {
		assertEquals("James Brown", IsiImporter.isiAuthorConvert("James Brown"));
		assertEquals("James Marc Brown", IsiImporter.isiAuthorConvert("James Marc Brown"));
		assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, J.M."));
		assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J."));
		assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, JM"));
		assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J"));
		assertEquals("", IsiImporter.isiAuthorConvert(""));
	}

	
	public void testGetExtensions() {
		// new IsiImporter().getExtensions();
	}

	public void testGetIsCustomImporter() {
		IsiImporter importer = new IsiImporter();
		assertEquals(false, importer.getIsCustomImporter());
	}
}
