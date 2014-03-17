package tests.net.sf.jabref.imports;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.imports.EntryFromPDFCreator;

/**
 * 
 * @version 11.11.2008 | 22:16
 * 
 */
public class EntryFromPDFCreatorTest extends TestCase {

	private EntryFromPDFCreator entryCreator;
	
	private File existingPDF;
	private File notExistingPDF;

	protected void setUp() throws Exception {
		// externalFileTypes are needed for the EntryFromPDFCreator      
		JabRefPreferences.getInstance().updateExternalFileTypes();

		
		entryCreator = new EntryFromPDFCreator();
		existingPDF = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfNotInDatabase.pdf");
		notExistingPDF = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder/null.pdf");
		
	}

	public void testPDFFileFilter() {

		Assert.assertEquals(true, entryCreator.accept(new File("aPDF.pdf")));
		Assert.assertEquals(true, entryCreator.accept(new File("aPDF.PDF")));
		Assert.assertEquals(false, entryCreator.accept(new File("foo.jpg")));
	}

	public void testCreationOfEntry() {
		
		BibtexEntry entry = entryCreator.createEntry(notExistingPDF, false);
		assertNull(entry);

		entry = entryCreator.createEntry(existingPDF, false);
		Assert.assertNotNull(entry);
		Assert.assertTrue(entry.getField("file").endsWith(":PDF"));
		Assert.assertEquals(existingPDF.getName(), entry.getField("title"));

	}
} 