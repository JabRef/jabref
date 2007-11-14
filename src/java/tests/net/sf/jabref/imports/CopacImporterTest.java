package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.imports.CopacImporter;

public class CopacImporterTest extends TestCase {
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

        CopacImporter importer = new CopacImporter();
		assertTrue(importer.isRecognizedFormat(CopacImporterTest.class
			.getResourceAsStream("CopacImporterTest1.txt")));

		assertTrue(importer.isRecognizedFormat(CopacImporterTest.class
			.getResourceAsStream("CopacImporterTest2.txt")));

		assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
			.getResourceAsStream("IsiImporterTest1.isi")));

		assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
			.getResourceAsStream("IsiImporterTestINSPEC.isi")));

		assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
			.getResourceAsStream("IsiImporterTestWOS.isi")));

		assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
			.getResourceAsStream("IsiImporterTestMedline.isi")));
	}

	public void testImportEntries() throws IOException {
		CopacImporter importer = new CopacImporter();

		List<BibtexEntry> entries = importer.importEntries(CopacImporterTest.class
			.getResourceAsStream("CopacImporterTest1.txt"));
		assertEquals(1, entries.size());
		BibtexEntry entry = entries.get(0);
		
		assertEquals("The SIS project : software reuse with a natural language approach", entry.getField("title"));
		assertEquals(
			"Prechelt, Lutz and Universität Karlsruhe. Fakultät für Informatik",
			entry.getField("author"));
		assertEquals("Interner Bericht ; Nr.2/92", entry.getField("series"));
		assertEquals("1992", entry.getField("year"));
		assertEquals("Karlsruhe :  Universitat Karlsruhe, Fakultat fur Informatik", entry.getField("publisher"));
		assertEquals(BibtexEntryType.BOOK, entry.getType());
	}

	public void testImportEntries2() throws IOException {
		CopacImporter importer = new CopacImporter();

		List<BibtexEntry> entries = importer.importEntries(CopacImporterTest.class
			.getResourceAsStream("CopacImporterTest2.txt"));
		assertEquals(2, entries.size());
		BibtexEntry one = entries.get(0);
		
		assertEquals("Computing and operational research at the London Hospital", one.getField("title"));
		
		BibtexEntry two = entries.get(1);
	
		assertEquals("Real time systems : management and design", two.getField("title"));
	}
}
