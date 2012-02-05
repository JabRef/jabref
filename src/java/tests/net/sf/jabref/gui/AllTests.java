package tests.net.sf.jabref.gui;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for tests.net.sf.jabref.gui");
		//$JUnit-BEGIN$
		suite.addTestSuite(AutoCompleterTest.class);
		//$JUnit-END$
		return suite;
	}

}
