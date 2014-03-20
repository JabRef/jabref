package net.sf.jabref.export.layout.format;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.AuthorLastFirstAbbreviator;

/**
 * Test case  that verifies the functionalities of the
 * formater AuthorLastFirstAbbreviator.
 * 
 * @author Carlos Silla
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class AuthorLastFirstAbbreviatorTester extends TestCase {

	/**
	 * Verifies the Abbreviation of one single author with a simple name.
	 * 
	 * Ex: Lastname, Name
	 */
	public void testOneAuthorSimpleName() {
        assertEquals("Abbreviator Test", "Lastname, N.", abbreviate("Lastname, Name"));
	}

	/**
	 * Verifies the Abbreviation of one single author with a common name.
	 * 
	 * Ex: Lastname, Name Middlename
	 */
	public void testOneAuthorCommonName() {
		assertEquals("Abbreviator Test", "Lastname, N. M.", abbreviate("Lastname, Name Middlename"));
	}

	/**
	 * Verifies the Abbreviation of two single with a common name.
	 * 
	 * Ex: Lastname, Name Middlename
	 */
	public void testTwoAuthorsCommonName() {
        String result = abbreviate("Lastname, Name Middlename and Sobrenome, Nome Nomedomeio");
		String expectedResult = "Lastname, N. M. and Sobrenome, N. N.";

		assertEquals("Abbreviator Test", expectedResult, result);
	}


	/**
	 * Testcase for 
	 * http://sourceforge.net/tracker/index.php?func=detail&aid=1466924&group_id=92314&atid=600306
	 */
	public void testJrAuthor(){
        //TODO what should be done here? reimplement it?
        assertEquals("Other, A. N.", abbreviate("Other, Jr., Anthony N."));
	}

	public void testFormat() {
		assertEquals("", abbreviate(""));
		assertEquals("Someone, V. S.", abbreviate("Someone, Van Something"));
		assertEquals("Smith, J.", abbreviate("Smith, John"));
		assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.",
                abbreviate("von Neumann, John and Smith, John and Black Brown, Peter"));
	}
	
	protected String abbreviate(String name) {
		return (new AuthorLastFirstAbbreviator()).format(name);
	}
	
}
