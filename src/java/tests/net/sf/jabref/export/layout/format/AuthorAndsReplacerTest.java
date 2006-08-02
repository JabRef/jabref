package tests.net.sf.jabref.export.layout.format;

import junit.framework.TestCase;
import net.sf.jabref.export.layout.format.AuthorAndsReplacer;

public class AuthorAndsReplacerTest extends TestCase {

	public void testFormat() {
		
		AuthorAndsReplacer a = new AuthorAndsReplacer();
		
		assertEquals("", a.format(""));
		assertEquals("John Smith", a.format("John Smith"));
		assertEquals("John Smith & Black Brown, Peter", a.format("John Smith and Black Brown, Peter"));
		assertEquals("John von Neumann; John Smith & Peter Black Brown",
				a.format("John von Neumann and John Smith and Peter Black Brown"));
	}

}
