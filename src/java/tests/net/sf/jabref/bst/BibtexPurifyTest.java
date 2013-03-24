package tests.net.sf.jabref.bst;

import net.sf.jabref.bst.BibtexPurify;
import net.sf.jabref.bst.Warn;
import junit.framework.TestCase;

public class BibtexPurifyTest extends TestCase {

	public void testPurify() {
		assertPurify("i", "i");
		assertPurify("0I  ", "0I~ ");
		assertPurify("Hi Hi ", "Hi Hi ");
		assertPurify("oe", "{\\oe}");
		assertPurify("Hi oeHi ", "Hi {\\oe   }Hi ");
		assertPurify("Jonathan Meyer and Charles Louis Xavier Joseph de la Vallee Poussin", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
		assertPurify("e", "{\\'e}");
		assertPurify("Edouard Masterly", "{\\'{E}}douard Masterly");
		assertPurify("Ulrich Underwood and Ned Net and Paul Pot", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");}

	private void assertPurify(final String string, final String string2) {
		assertEquals(string, BibtexPurify.purify(string2, new Warn() {
			public void warn(String s) {
				fail("Should not Warn ("+s+")! purify should be " + string + " for " + string2);
			}
		}));
	}
}
