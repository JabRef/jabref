package tests.net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.format.AuthorLastFirstAbbreviator;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test case  that verifies the functionalities of the
 * formater AuthorLastFirstAbbreviator.
 * 
 * @author Carlos Silla
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
	//TODO: Verify how to tell this test that it should pass if fail.
/*	public void testTwoAuthorsBadFormating() {
		String name = new String("Lastname, Name Middlename and Nome Nomedomeio Sobrenome");
		
		AuthorLastFirstAbbreviator ab = new AuthorLastFirstAbbreviator();
		
		String result = ab.format(name);		
	}*/
	
}
