package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.imports.IsiImporter;
import net.sf.jabref.imports.RisImporter;

/**
 * Test cases for the RISImporter
 * 
 * @author $Author: coezbek $
 * 
 */
public class RISImporterTest extends TestCase {

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

        RisImporter importer = new RisImporter();
		assertTrue(importer.isRecognizedFormat(RISImporterTest.class
			.getResourceAsStream("RisImporterTest1.ris")));
	}

	public void testProcessSubSup() {

		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("title", "/sub 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_3$", hm.get("title"));

		hm.put("title", "/sub   3   /");
		IsiImporter.processSubSup(hm);
		assertEquals("$_3$", hm.get("title"));

		hm.put("title", "/sub 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_{31}$", hm.get("title"));

		hm.put("abstract", "/sub 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_3$", hm.get("abstract"));

		hm.put("review", "/sub 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_{31}$", hm.get("review"));

		hm.put("title", "/sup 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^3$", hm.get("title"));

		hm.put("title", "/sup 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^{31}$", hm.get("title"));

		hm.put("abstract", "/sup 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^3$", hm.get("abstract"));

		hm.put("review", "/sup 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^{31}$", hm.get("review"));

		hm.put("title", "/sub $Hello/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_{\\$Hello}$", hm.get("title"));
	}

	public void testImportEntries() throws IOException {
		RisImporter importer = new RisImporter();

		List<BibtexEntry> entries = importer.importEntries(RISImporterTest.class
			.getResourceAsStream("RisImporterTest1.ris"));
		assertEquals(1, entries.size());
		BibtexEntry entry = entries.get(0);
		assertEquals("Editorial: Open Source and Empirical Software Engineering", entry
			.getField("title"));
		assertEquals(
			"Harrison, Warren",
			entry.getField("author"));

		assertEquals(BibtexEntryType.ARTICLE, entry.getType());
		assertEquals("Empirical Software Engineering", entry.getField("journal"));
		assertEquals("2001", entry.getField("year"));
		assertEquals("6", entry.getField("volume"));
		assertEquals("3", entry.getField("number"));
		assertEquals("193--194", entry.getField("pages"));
		assertEquals("#sep#", entry.getField("month"));
	}
}
