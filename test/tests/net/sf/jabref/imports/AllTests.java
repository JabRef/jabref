package tests.net.sf.jabref.imports;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for tests.net.sf.jabref.imports");
		//$JUnit-BEGIN$
		suite.addTestSuite(OAI2ImportTest.class);
		suite.addTestSuite(IsiImporterTest.class);
		suite.addTestSuite(CopacImporterTest.class);
		suite.addTestSuite(BibtexParserTest.class);
		suite.addTestSuite(GeneralFetcherTest.class);
		//$JUnit-END$
		return suite;
	}

}
