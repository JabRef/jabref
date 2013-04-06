package tests.net.sf.jabref.export.layout;

import junit.framework.TestCase;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.RTFChars;

public class RTFCharsTest extends TestCase {

	public void testBasicFormat() {

		LayoutFormatter layout = new RTFChars();

		assertEquals("", layout.format(""));

		assertEquals("hallo", layout.format("hallo"));

		// We should be able to replace the ? with e
		assertEquals("R\\u233?flexions sur le timing de la quantit\\u233?", layout.format("Réflexions sur le timing de la quantité"));

		assertEquals("h\\u225allo", layout.format("h\\'allo"));
		assertEquals("h\\u225allo", layout.format("h\\'allo"));
	}

	public void testLaTeXHighlighting(){
		
		LayoutFormatter layout = new RTFChars();
		
		assertEquals("{\\i hallo}", layout.format("\\emph{hallo}"));
		assertEquals("{\\i hallo}", layout.format("{\\emph hallo}"));

		assertEquals("{\\i hallo}", layout.format("\\textit{hallo}"));
		assertEquals("{\\i hallo}", layout.format("{\\textit hallo}"));

		assertEquals("{\\b hallo}", layout.format("\\textbf{hallo}"));
		assertEquals("{\\b hallo}", layout.format("{\\textbf hallo}"));
	}
	
	public void testComplicated() {
		LayoutFormatter layout = new RTFChars();

		assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e \\u230ae should be \\u230ae", layout.format("Réflexions sur le timing de la quantité \\ae should be æ"));

		assertEquals("h\\u225all{\\uc2\\u339oe}", layout.format("h\\'all\\oe "));
	}

	public void testSpecialCharacters() {

		LayoutFormatter layout = new RTFChars();

		assertEquals("\\u243o", layout.format("\\'{o}")); // ó
		assertEquals("\\'f2", layout.format("\\`{o}")); // ò
		assertEquals("\\'f4", layout.format("\\^{o}")); // ô
		assertEquals("\\'f6", layout.format("\\\"{o}")); // ö
		assertEquals("\\u245o", layout.format("\\~{o}")); // õ
		assertEquals("\\u333o", layout.format("\\={o}"));
		assertEquals("\\u334O", layout.format("\\u{o}"));
		assertEquals("\\u231c", layout.format("\\c{c}")); // ç
		assertEquals("{\\uc2\\u339oe}", layout.format("\\oe"));
		assertEquals("{\\uc2\\u338OE}", layout.format("\\OE"));
		assertEquals("{\\uc2\\u230ae}", layout.format("\\ae")); // æ
		assertEquals("{\\uc2\\u198AE}", layout.format("\\AE")); // Æ

		assertEquals("", layout.format("\\.{o}")); // ???
		assertEquals("", layout.format("\\v{o}")); // ???
		assertEquals("", layout.format("\\H{a}")); // ã // ???
		assertEquals("", layout.format("\\t{oo}"));
		assertEquals("", layout.format("\\d{o}")); // ???
		assertEquals("", layout.format("\\b{o}")); // ???
		assertEquals("", layout.format("\\aa")); // å
		assertEquals("", layout.format("\\AA")); // Å
		assertEquals("", layout.format("\\o")); // ø
		assertEquals("", layout.format("\\O")); // Ø
		assertEquals("", layout.format("\\l"));
		assertEquals("", layout.format("\\L"));
		assertEquals("{\\uc2\\u223ss}", layout.format("\\ss")); // ß
		assertEquals("", layout.format("?`")); // ¿
		assertEquals("", layout.format("!`")); // ¡

		assertEquals("", layout.format("\\dag"));
		assertEquals("", layout.format("\\ddag"));
		assertEquals("", layout.format("\\S")); // §
		assertEquals("", layout.format("\\P")); // ¶
		assertEquals("", layout.format("\\copyright")); // ©
		assertEquals("", layout.format("\\pounds")); // £
	}
}
