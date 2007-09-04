package tests.net.sf.jabref;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for test.net.sf.jabref");
		//$JUnit-BEGIN$
		suite.addTestSuite(BibtexDatabaseTest.class);
		suite.addTestSuite(JabRefTestCase.class);
		suite.addTestSuite(UtilFindFileTest.class);
		suite.addTestSuite(AuthorListTest.class);
		suite.addTestSuite(FileBasedTestCase.class);
		suite.addTestSuite(UtilTest.class);
		//$JUnit-END$

		suite.addTest(tests.net.sf.jabref.export.layout.format.AllTests.suite());
		suite.addTest(tests.net.sf.jabref.imports.AllTests.suite());
		suite.addTest(tests.net.sf.jabref.search.AllTests.suite());
		suite.addTest(tests.net.sf.jabref.util.AllTests.suite());
		suite.addTest(tests.net.sf.jabref.export.layout.AllTests.suite());
		suite.addTest(tests.net.sf.jabref.bst.AllTests.suite());
		suite.addTest(tests.net.sf.jabref.labelPattern.AllTests.suite());

		return suite;
	}

}
