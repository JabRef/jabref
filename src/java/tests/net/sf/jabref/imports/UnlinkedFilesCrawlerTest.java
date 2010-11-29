package tests.net.sf.jabref.imports;

import java.io.File;

import junit.framework.TestCase;

/**
 * 
 * @author Nosh&Dan
 * @version 09.11.2008 | 19:41:40
 * 
 */
public class UnlinkedFilesCrawlerTest extends TestCase {

	private File fileInDatabase;
	private File fileNotInDatabase;
	
	private File existingFolder;
	private File notExistingFolder;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		fileInDatabase = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfInDatabase.pdf");
		fileNotInDatabase = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfNotInDatabase.pdf");
		
		existingFolder = new File("src/tests/net/sf/jabref/imports/unlinkedFilesTestFolder");
		notExistingFolder = new File("notexistingfolder");
	}
	
	/**
	 * Tests the testing environment.
	 */
	public void testTestingEnvironment() {
		
		assertNotNull(existingFolder);
		assertTrue(existingFolder.exists());
		assertTrue(existingFolder.isDirectory());
		
		assertTrue(fileInDatabase.exists());
		assertTrue(fileInDatabase.isFile());
		
		assertTrue(fileNotInDatabase.exists());
		assertTrue(fileNotInDatabase.isFile());
		
	}
	
	public void testOpenNotExistingDirectory() {

		assertNotNull(notExistingFolder);
		assertFalse(notExistingFolder.exists());
		
	}

}
