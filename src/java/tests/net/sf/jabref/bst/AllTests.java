package tests.net.sf.jabref.bst;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for tests.net.sf.jabref.bst.test");
		//$JUnit-BEGIN$
		suite.addTestSuite(BibtexNameFormatterTest.class);
		suite.addTestSuite(BibtexCaseChangerTest.class);
		suite.addTestSuite(TestVM.class);
		suite.addTestSuite(BibtexWidthTest.class);
		suite.addTestSuite(TextPrefixFunctionTest.class);
		suite.addTestSuite(BibtexPurifyTest.class);
		//$JUnit-END$
		return suite;
	}

}
