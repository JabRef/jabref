package tests.net.sf.jabref.imports;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.EntryFromFileCreator;
import net.sf.jabref.imports.EntryFromFileCreatorManager;
import net.sf.jabref.imports.ParserResult;

/**
 * 
 * @version 11.11.2008 | 21:51:54
 * 
 */
public class EntryFromFileCreatorManagerTest extends TestCase {
	
	private BibtexDatabase database;

	private File existingFile;
	private File notExistingFile;
	
	private File pdfNotInDatabase;
	
	EntryFromFileCreatorManager manager1;
	EntryFromFileCreatorManager manager2;

	protected void setUp() throws Exception {
		super.setUp();
		
		existingFile = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfInDatabase.pdf");
		notExistingFile = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder/null.pdf");
		
		pdfNotInDatabase = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfNotInDatabase.pdf");

		manager1 = new EntryFromFileCreatorManager();
		ParserResult result = BibtexParser.parse(new FileReader("src/tests/net/sf/jabref/util/unlinkedFilesTestBib.bib"));
		database = result.getDatabase();
	}

	public void testGetCreator() throws Exception {
		
		EntryFromFileCreator creator = manager1.getEntryCreator(notExistingFile);
		assertNull(creator);
		
		creator = manager1.getEntryCreator(existingFile);
		assertNotNull(creator);
		assertTrue(creator.accept(existingFile));
	}

	public void testAddEntrysFromFiles() throws Exception {
		List<File> files = new ArrayList<File>();

		files.add(pdfNotInDatabase);
		files.add(notExistingFile);

		manager2 = new EntryFromFileCreatorManager();
		List<String> errors = manager2.addEntrysFromFiles(files, database, null, true);
		
		/**
		 * One file doesn't exist, so adding it as an entry should lead to an
		 * error message.
		 */
		assertEquals(1, errors.size());

		boolean file1Found = false, file2Found = false;
		for (BibtexEntry entry : database.getEntries()) {
			String filesInfo = entry.getField("file");
			if (filesInfo.contains(files.get(0).getName())) {
				file1Found = true;
			}
			if (filesInfo.contains(files.get(1).getName())) {
				file2Found = true;
			}
		}

		assertTrue(file1Found);
		assertFalse(file2Found);
	}

}
