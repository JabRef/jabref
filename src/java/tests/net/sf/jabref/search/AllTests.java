package tests.net.sf.jabref.search;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for tests.net.sf.jabref.search");
		//$JUnit-BEGIN$
		suite.addTestSuite(BasicSearchTest.class);
		//$JUnit-END$
		return suite;
	}

}
