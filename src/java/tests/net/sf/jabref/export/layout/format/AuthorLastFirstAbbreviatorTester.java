package tests.net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.format.AuthorLastFirstAbbreviator;
import junit.framework.Assert;
import junit.framework.TestCase;

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
		String expectedResult = "Lastname, N.M.";
		
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
		String expectedResult = "Lastname, N.M. and Sobrenome, N.N.";
		
		//Verifies the functionality:				
		Assert.assertEquals("Abbreviator Test", result, expectedResult);
	}


	/**
	 * Verifies the Abbreviation of two authors in the incorrect format.
	 * 
	 * Ex: Lastname, Name Middlename
	 */
	public void testTwoAuthorsBadFormating() {
		// String name = new String("Lastname, Name Middlename and Nome Nomedomeio Sobrenome");
		
		fail();
		// @TODO: How should a Formatter fail? 
		// assertEquals("Author names must be formatted \"Last, First\" or \"Last, Jr., First\" before formatting with AuthorLastFirstAbbreviator", abbreviate(name));
	}
	
	/**
	 * Testcase for 
	 * http://sourceforge.net/tracker/index.php?func=detail&aid=1466924&group_id=92314&atid=600306
	 */
	public void testJrAuthor(){
		String name = "Other, Jr., Anthony N.";
		assertEquals("Other, A.N.", abbreviate(name));
	}

	protected String abbreviate(String name) {
		return (new AuthorLastFirstAbbreviator()).format(name);
	}
	
}
