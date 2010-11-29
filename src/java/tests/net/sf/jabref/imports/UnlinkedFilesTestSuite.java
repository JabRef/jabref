package tests.net.sf.jabref.imports;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs all relevant test for the feature "Find unlinked files".
 * 
 * @author Nosh&Dan
 * @version 14.11.2008 | 00:26:19
 * 
 */
public class UnlinkedFilesTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for the feature \"Find unlinked files\".");
		suite.addTestSuite(DatabaseFileLookupTest.class);
		suite.addTestSuite(EntryFromFileCreatorManagerTest.class);
		suite.addTestSuite(EntryFromPDFCreatorTest.class);
		suite.addTestSuite(UnlinkedFilesCrawlerTest.class);
		return suite;
	}

}
