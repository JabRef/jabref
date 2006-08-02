package tests.net.sf.jabref;

import junit.framework.Test;
import junit.framework.TestSuite;
import tests.net.sf.jabref.export.layout.format.AuthorAndsReplacerTest;
import tests.net.sf.jabref.export.layout.format.AuthorLastFirstAbbreviatorTester;
import tests.net.sf.jabref.imports.BibtexParserTest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for test.net.sf.jabref");
		//$JUnit-BEGIN$
		suite.addTestSuite(AuthorListTest.class);
		suite.addTestSuite(UtilTest.class);
		suite.addTestSuite(AuthorAndsReplacerTest.class);
		suite.addTestSuite(AuthorLastFirstAbbreviatorTester.class);
		suite.addTestSuite(BibtexParserTest.class);
		//$JUnit-END$
		return suite;
	}

}
