package tests.net.sf.jabref.export.layout.format;

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
		String name = "Lastname, Name";
		
		AuthorLastFirstAbbreviator ab = new AuthorLastFirstAbbreviator();
		
		String result = ab.format(name);
		
		//Expected Results:
		String expectedResult = "Lastname, N.";

		//Verifies the functionality:				
		Assert.assertEquals("Abbreviator Test", result, expectedResult);
	}

	/**
	 * Verifies the Abbreviation of one single author with a common name.
	 * 
	 * Ex: Lastname, Name Middlename
	 */
	public void testOneAuthorCommonName() {
		String name = "Lastname, Name Middlename";
		
		AuthorLastFirstAbbreviator ab = new AuthorLastFirstAbbreviator();
		
		String result = ab.format(name);
		
		//Expected Results:
		String expectedResult = "Lastname, N. M.";
		
		//Verifies the functionality:				
		Assert.assertEquals("Abbreviator Test", result, expectedResult);
	}

	/**
	 * Verifies the Abbreviation of two single with a common name.
	 * 
	 * Ex: Lastname, Name Middlename
	 */
	public void testTwoAuthorsCommonName() {
		String name = "Lastname, Name Middlename and Sobrenome, Nome Nomedomeio";
		
		AuthorLastFirstAbbreviator ab = new AuthorLastFirstAbbreviator();
		
		String result = ab.format(name);
		
		//Expected Results:
		String expectedResult = "Lastname, N. M. and Sobrenome, N. N.";
		
		//Verifies the functionality:				
		Assert.assertEquals("Abbreviator Test", result, expectedResult);
	}


	/**
	 * Testcase for 
	 * http://sourceforge.net/tracker/index.php?func=detail&aid=1466924&group_id=92314&atid=600306
	 */
	public void testJrAuthor(){
		String name = "Other, Jr., Anthony N.";
		assertEquals("Other, A. N.", abbreviate(name));
	}

	public void testFormat() {

		LayoutFormatter a = new AuthorLastFirstAbbreviator();
		
		assertEquals("", a.format(""));
		assertEquals("Someone, V. S.", a.format("Someone, Van Something"));
		assertEquals("Smith, J.", a.format("Smith, John"));
		assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.",
				a.format("von Neumann, John and Smith, John and Black Brown, Peter"));
		
	}
	
	protected String abbreviate(String name) {
		return (new AuthorLastFirstAbbreviator()).format(name);
	}
	
}
