package tests.net.sf.jabref.export.layout.format;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for test.net.sf.jabref");
		//$JUnit-BEGIN$
		suite.addTestSuite(AuthorFirstAbbrLastOxfordCommasTest.class);
		suite.addTestSuite(AuthorFirstLastCommasTest.class);
		suite.addTestSuite(AuthorLastFirstAbbreviatorTester.class);
		suite.addTestSuite(AuthorLastFirstTest.class);
		suite.addTestSuite(AuthorAndsCommaReplacerTest.class);
		suite.addTestSuite(AuthorFirstFirstTest.class);
		suite.addTestSuite(AuthorFirstLastOxfordCommasTest.class);
		suite.addTestSuite(AuthorOrgSciTest.class);
		suite.addTestSuite(AuthorAbbreviatorTest.class);
		suite.addTestSuite(CompositeFormatTest.class);
		suite.addTestSuite(ResolvePDFTest.class);
		suite.addTestSuite(AuthorAndsReplacerTest.class);
		suite.addTestSuite(AuthorLastFirstAbbrCommasTest.class);
		suite.addTestSuite(HTMLParagraphsTest.class);
		suite.addTestSuite(AuthorLastFirstOxfordCommasTest.class);
		suite.addTestSuite(AuthorFirstAbbrLastCommasTest.class);
		suite.addTestSuite(AuthorLastFirstCommasTest.class);
		suite.addTestSuite(AuthorLastFirstAbbrOxfordCommasTest.class);
		suite.addTestSuite(NoSpaceBetweenAbbreviationsTest.class);
		//$JUnit-END$
		return suite;
	}

}
