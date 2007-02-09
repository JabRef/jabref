package tests.net.sf.jabref.export.layout;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for test.net.sf.jabref.layout");
		//$JUnit-BEGIN$
		suite.addTestSuite(HTMLCharsTest.class);
		suite.addTestSuite(RTFCharsTest.class);
		suite.addTestSuite(LayoutTest.class);
		//$JUnit-END$
		return suite;
	}

}
