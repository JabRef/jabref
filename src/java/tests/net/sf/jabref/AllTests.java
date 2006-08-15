package tests.net.sf.jabref;

import junit.framework.Test;
import junit.framework.TestSuite;
import tests.net.sf.jabref.export.layout.format.AuthorAndsReplacerTest;
import tests.net.sf.jabref.export.layout.format.AuthorLastFirstAbbreviatorTester;
import tests.net.sf.jabref.imports.BibtexParserTest;
import tests.net.sf.jabref.util.CaseChangerTest;
import tests.net.sf.jabref.util.XMPUtilTest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for test.net.sf.jabref");
		//$JUnit-BEGIN$
		suite.addTestSuite(AuthorListTest.class);
		suite.addTestSuite(UtilTest.class);
		suite.addTestSuite(UtilFindFileTest.class);
		suite.addTest(tests.net.sf.jabref.export.layout.format.AllTests.suite());
		suite.addTestSuite(BibtexParserTest.class);
		suite.addTestSuite(CaseChangerTest.class);
		suite.addTestSuite(XMPUtilTest.class);
		//$JUnit-END$
		return suite;
	}

}
