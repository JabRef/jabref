package tests.net.sf.jabref.bst;

import net.sf.jabref.bst.BibtexTextPrefix;
import net.sf.jabref.bst.Warn;
import junit.framework.TestCase;

public class TextPrefixFunctionTest extends TestCase {
	
	public void testPrefix(){
		assertPrefix("i", "i");
		assertPrefix("0I~ ", "0I~ ");
		assertPrefix("Hi Hi", "Hi Hi ");
		assertPrefix("{\\oe}", "{\\oe}");
		assertPrefix("Hi {\\oe   }H", "Hi {\\oe   }Hi ");
		assertPrefix("Jonat", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
		assertPrefix("{\\'e}", "{\\'e}");
		assertPrefix("{\\'{E}}doua", "{\\'{E}}douard Masterly");
		assertPrefix("Ulric", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
	}

	private void assertPrefix(final String string, final String string2) {
		assertEquals(string, BibtexTextPrefix.textPrefix(5, string2, new Warn() {
			public void warn(String s) {
				fail("Should not Warn! text.prefix$ should be " + string + " for (5) " + string2);
			}
		}));
	}

}
