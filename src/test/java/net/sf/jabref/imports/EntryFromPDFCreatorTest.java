package net.sf.jabref.imports;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefPreferences;

/**
 * 
 * @version 11.11.2008 | 22:16
 * 
 */
public class EntryFromPDFCreatorTest extends TestCase {

	private EntryFromPDFCreator entryCreator = new EntryFromPDFCreator();

	protected void setUp() throws Exception {
		// externalFileTypes are needed for the EntryFromPDFCreator
		JabRefPreferences.getInstance().updateExternalFileTypes();
	}

	public void testPDFFileFilter() {
		Assert.assertTrue(entryCreator.accept(new File("aPDF.pdf")));
		Assert.assertTrue(entryCreator.accept(new File("aPDF.PDF")));
		Assert.assertFalse(entryCreator.accept(new File("foo.jpg")));
	}

	public void testCreationOfEntry() {
		BibtexEntry entry = entryCreator.createEntry(ImportDataTest.NOT_EXISTING_PDF, false);
		assertNull(entry);

		entry = entryCreator.createEntry(ImportDataTest.FILE_NOT_IN_DATABASE, false);
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.getField("file").endsWith(":PDF"));
		Assert.assertEquals(ImportDataTest.FILE_NOT_IN_DATABASE.getName(), entry.getField("title"));

	}
} 