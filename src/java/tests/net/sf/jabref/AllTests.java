package tests.net.sf.jabref;

import junit.framework.Test;
import junit.framework.TestSuite;
import tests.net.sf.jabref.export.layout.format.AuthorAndsReplacerTest;
import tests.net.sf.jabref.export.layout.format.AuthorLastFirstAbbreviatorTester;
import tests.net.sf.jabref.imports.BibtexParserTest;
import tests.net.sf.jabref.util.CaseChangerTest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for test.net.sf.jabref");
		//$JUnit-BEGIN$
		suite.addTestSuite(AuthorListTest.class);
		suite.addTestSuite(UtilTest.class);
		suite.addTestSuite(UtilFindFileTest.class);
		suite.addTestSuite(AuthorAndsReplacerTest.class);
		suite.addTestSuite(AuthorLastFirstAbbreviatorTester.class);
		suite.addTestSuite(BibtexParserTest.class);
		suite.addTestSuite(CaseChangerTest.class);
		//$JUnit-END$
		return suite;
	}

}
