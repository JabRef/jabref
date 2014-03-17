package tests.net.sf.jabref.bst;

import net.sf.jabref.bst.BibtexCaseChanger;
import net.sf.jabref.bst.Warn;
import junit.framework.TestCase;

public class BibtexCaseChangerTest extends TestCase {

	public void testChangeCase() {

		assertCaseChangerT("i", "i");
		assertCaseChangerL("i", "i");
		assertCaseChangerU("I", "i");
		assertCaseChangerT("0i~ ", "0I~ ");
		assertCaseChangerL("0i~ ", "0I~ ");
		assertCaseChangerU("0I~ ", "0I~ ");
		assertCaseChangerT("Hi hi ", "Hi Hi ");
		assertCaseChangerL("hi hi ", "Hi Hi ");
		assertCaseChangerU("HI HI ", "Hi Hi ");
		assertCaseChangerT("{\\oe}", "{\\oe}");
		assertCaseChangerL("{\\oe}", "{\\oe}");
		assertCaseChangerU("{\\OE}", "{\\oe}");
		assertCaseChangerT("Hi {\\oe   }hi ", "Hi {\\oe   }Hi ");
		assertCaseChangerL("hi {\\oe   }hi ", "Hi {\\oe   }Hi ");
		assertCaseChangerU("HI {\\OE   }HI ", "Hi {\\oe   }Hi ");
		assertCaseChangerT(
			"Jonathan meyer and charles louis xavier joseph de la vall{\\'e}e poussin",
			"Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
		assertCaseChangerL(
			"jonathan meyer and charles louis xavier joseph de la vall{\\'e}e poussin",
			"Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
		assertCaseChangerU(
			"JONATHAN MEYER AND CHARLES LOUIS XAVIER JOSEPH DE LA VALL{\\'E}E POUSSIN",
			"Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
		assertCaseChangerT("{\\'e}", "{\\'e}");
		assertCaseChangerL("{\\'e}", "{\\'e}");
		assertCaseChangerU("{\\'E}", "{\\'e}");
		assertCaseChangerT("{\\'{E}}douard masterly", "{\\'{E}}douard Masterly");
		assertCaseChangerL("{\\'{e}}douard masterly", "{\\'{E}}douard Masterly");
		assertCaseChangerU("{\\'{E}}DOUARD MASTERLY", "{\\'{E}}douard Masterly");
		assertCaseChangerT("Ulrich {\\\"{u}}nderwood and ned {\\~n}et and paul {\\={p}}ot",
			"Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
		assertCaseChangerL("ulrich {\\\"{u}}nderwood and ned {\\~n}et and paul {\\={p}}ot",
			"Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
		assertCaseChangerU("ULRICH {\\\"{U}}NDERWOOD AND NED {\\~N}ET AND PAUL {\\={P}}OT",
			"Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
		assertCaseChangerT("An {$O(n \\log n / \\! \\log\\log n)$} sorting algorithm",
			"An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm");
		assertCaseChangerL("an {$O(n \\log n / \\! \\log\\log n)$} sorting algorithm",
			"An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm");
		assertCaseChangerU("AN {$O(n \\log n / \\! \\log\\log n)$} SORTING ALGORITHM",
			"An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm");

		assertCaseChangerT("hallo", "hallo");
		assertCaseChangerT("Hallo", "HAllo");
		assertCaseChangerT("Hallo world", "HAllo World");
		assertCaseChangerT("Hallo world. how", "HAllo WORLD. HOW");
		assertCaseChangerT("Hallo {WORLD}. how", "HAllo {WORLD}. HOW");
		assertCaseChangerT("Hallo {\\world}. how", "HAllo {\\WORLD}. HOW");

		assertCaseChangerL("hallo", "hallo");
		assertCaseChangerL("hallo", "HAllo");
		assertCaseChangerL("hallo world", "HAllo World");
		assertCaseChangerL("hallo world. how", "HAllo WORLD. HOW");
		assertCaseChangerL("hallo {worLD}. how", "HAllo {worLD}. HOW");
		assertCaseChangerL("hallo {\\world}. how", "HAllo {\\WORLD}. HOW");

		assertCaseChangerU("HALLO", "hallo");
		assertCaseChangerU("HALLO", "HAllo");
		assertCaseChangerU("HALLO WORLD", "HAllo World");
		assertCaseChangerU("HALLO WORLD. HOW", "HAllo World. How");
		assertCaseChangerU("HALLO {worLD}. HOW", "HAllo {worLD}. how");
		assertCaseChangerU("HALLO {\\WORLD}. HOW", "HAllo {\\woRld}. hoW");

		assertCaseChangerT("On notions of information transfer in {VLSI} circuits",
			"On Notions of Information Transfer in {VLSI} Circuits");

	}

	public void testColon() {

		assertCaseChangerT("Hallo world: How", "HAllo WORLD: HOW");
		assertCaseChangerT("Hallo world! how", "HAllo WORLD! HOW");
		assertCaseChangerT("Hallo world? how", "HAllo WORLD? HOW");
		assertCaseChangerT("Hallo world. how", "HAllo WORLD. HOW");
		assertCaseChangerT("Hallo world, how", "HAllo WORLD, HOW");
		assertCaseChangerT("Hallo world; how", "HAllo WORLD; HOW");
		assertCaseChangerT("Hallo world- how", "HAllo WORLD- HOW");
	}

	private void assertCaseChangerT(final String string, final String string2) {
		assertEquals(string, BibtexCaseChanger.changeCase(string2, 't', new Warn() {

			public void warn(String s) {
				fail("Should not Warn (" + s + ")! changeCase('t') should be " + string + " for "
					+ string2);

			}
		}));

	}

	private void assertCaseChangerL(final String string, final String string2) {
		assertEquals(string, BibtexCaseChanger.changeCase(string2, 'l', new Warn() {

			public void warn(String s) {
				fail("Should not Warn (" + s + ")! changeCase('l') should be " + string + " for "
					+ string2);

			}
		}));

	}

	private void assertCaseChangerU(final String string, final String string2) {
		assertEquals(string, BibtexCaseChanger.changeCase(string2, 'u', new Warn() {
			public void warn(String s) {
				fail("Should not Warn (" + s + ")! changeCase('u') should be " + string + " for "
					+ string2);

			}
		}));

	}
}
