package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

/**
 * Test the BibtexParser
 * 
 * @version $revision: 1.1$ $date: $
 * 
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class BibtexParserTest extends TestCase {

	public void testParseReader() throws IOException {

		ParserResult result = BibtexParser.parse(new StringReader(
			"@article{test,author={Ed von Test}}"));

		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = (BibtexEntry) c.iterator().next();
		assertEquals("test", e.getCiteKey());
		assertEquals(2, e.getAllFields().length);
		Object[] o = e.getAllFields();
		assertTrue(o[0].toString().equals("author") || o[1].toString().equals("author"));
		assertEquals("Ed von Test", e.getField("author"));
	}

	public void testBibtexParser() {
		try {
			BibtexParser p = new BibtexParser(null);
			fail("Should not accept null.");
		} catch (NullPointerException npe) {

		}
	}

	public void testIsRecognizedFormat() throws IOException {
		assertTrue(BibtexParser
			.isRecognizedFormat(new StringReader(
				"This file was created with JabRef 2.1 beta 2."
					+ "\n"
					+ "Encoding: Cp1252"
					+ "\n"
					+ ""
					+ "\n"
					+ "@INPROCEEDINGS{CroAnnHow05,"
					+ "\n"
					+ "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
					+ "\n"
					+ "  title = {Effective work practices for floss development: A model and propositions},"
					+ "\n"
					+ "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
					+ "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
					+ "  timestamp = {2006.05.29}," + "\n"
					+ "  url = {http://james.howison.name/publications.html}" + "\n" + "}))")));

		assertTrue(BibtexParser.isRecognizedFormat(new StringReader(
			"This file was created with JabRef 2.1 beta 2." + "\n" + "Encoding: Cp1252" + "\n")));

		assertTrue(BibtexParser
			.isRecognizedFormat(new StringReader(
				"@INPROCEEDINGS{CroAnnHow05,"
					+ "\n"
					+ "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
					+ "\n"
					+ "  title = {Effective work practices for floss development: A model and propositions},"
					+ "\n"
					+ "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
					+ "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
					+ "  timestamp = {2006.05.29}," + "\n"
					+ "  url = {http://james.howison.name/publications.html}" + "\n" + "}))")));

		assertFalse(BibtexParser
			.isRecognizedFormat(new StringReader(
				"  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
					+ "\n"
					+ "  title = {Effective work practices for floss development: A model and propositions},"
					+ "\n"
					+ "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
					+ "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
					+ "  timestamp = {2006.05.29}," + "\n"
					+ "  url = {http://james.howison.name/publications.html}" + "\n" + "}))")));

		assertFalse(BibtexParser.isRecognizedFormat(new StringReader(
			"This was created with JabRef 2.1 beta 2." + "\n" + "Encoding: Cp1252" + "\n")));
	}

	public void testFromString() throws Exception {

		{ // Simple case
			Collection c = BibtexParser.fromString("@article{test,author={Ed von Test}}");
			assertEquals(1, c.size());

			BibtexEntry e = (BibtexEntry) c.iterator().next();
			assertEquals("test", e.getCiteKey());
			assertEquals(2, e.getAllFields().length);
			Object[] o = e.getAllFields();
			assertTrue(o[0].toString().equals("author") || o[1].toString().equals("author"));
			assertEquals("Ed von Test", e.getField("author"));
		}
		{ // Empty String
			Collection c = BibtexParser.fromString("");
			assertEquals(0, c.size());

		}
		{ // Error
			Collection c = BibtexParser.fromString("@@article@@{{{{{{}");
			assertEquals(null, c);
		}

	}

	public void testFromSingle2() {
		/**
		 * More
		 */
		Collection c = BibtexParser.fromString("@article{canh05,"
			+ "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n"
			+ "@inProceedings{foo," + "  author={Norton Bar}}");

		assertEquals(2, c.size());

		Iterator i = c.iterator();
		BibtexEntry a = (BibtexEntry) i.next();
		BibtexEntry b = (BibtexEntry) i.next();

		if (a.getCiteKey().equals("foo")) {
			BibtexEntry tmp = a;
			a = b;
			b = tmp;
		}

		assertEquals("canh05", a.getCiteKey());
		assertEquals("Crowston, K. and Annabi, H.", a.getField("author"));
		assertEquals("Title A", a.getField("title"));
		assertEquals(BibtexEntryType.ARTICLE, a.getType());

		assertEquals("foo", b.getCiteKey());
		assertEquals("Norton Bar", b.getField("author"));
		assertEquals(BibtexEntryType.INPROCEEDINGS, b.getType());
	}

	public void testFromStringSingle() {
		BibtexEntry a = BibtexParser.singleFromString("@article{canh05,"
			+ "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n");

		assertEquals("canh05", a.getCiteKey());
		assertEquals("Crowston, K. and Annabi, H.", a.getField("author"));
		assertEquals("Title A", a.getField("title"));
		assertEquals(BibtexEntryType.ARTICLE, a.getType());
		
		BibtexEntry b = BibtexParser.singleFromString("@article{canh05,"
			+ "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n"
			+ "@inProceedings{foo," + "  author={Norton Bar}}");

		if (!(b.getCiteKey().equals("canh05") || b.getCiteKey().equals("foo"))){
			fail();
		}
	}

	public void testParse() throws IOException {

		// Test Standard parsing
		BibtexParser parser = new BibtexParser(new StringReader(
			"@article{test,author={Ed von Test}}"));
		ParserResult result = parser.parse();

		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = (BibtexEntry) c.iterator().next();
		assertEquals("test", e.getCiteKey());
		assertEquals(2, e.getAllFields().length);
		Object[] o = e.getAllFields();
		assertTrue(o[0].toString().equals("author") || o[1].toString().equals("author"));
		assertEquals("Ed von Test", e.getField("author"));

		// Calling parse again will return the same result
		assertEquals(result, parser.parse());
	}

	public void testParse2() throws IOException {

		BibtexParser parser = new BibtexParser(new StringReader(
			"@article{test,author={Ed von Test}}"));
		ParserResult result = parser.parse();

		BibtexEntry e = new BibtexEntry("", BibtexEntryType.ARTICLE);
		e.setField("author", "Ed von Test");
		e.setField("bibtexkey", "test");

		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e2 = (BibtexEntry) c.iterator().next();

		assertNotSame(e.getId(), e2.getId());

		Object[] o = e.getAllFields();
		for (int i = 0; i < o.length; i++) {
			if (!e.getField(o[i].toString()).equals(e2.getField(o[i].toString()))) {
				fail("e and e2 differ in field " + o[i].toString());
			}
		}
	}

	/**
	 * Test for [ 1594123 ] Failure to import big numbers
	 * 
	 * Issue Reported by Ulf Martin.
	 * 
	 * @throws IOException
	 */
	public void testBigNumbers() throws IOException {

		ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,"
			+ "isbn = 1234567890123456789,\n" + "isbn2 = {1234567890123456789},\n"
			+ "small = 1234,\n" + "}"));

		Collection c = result.getDatabase().getEntries();
		BibtexEntry e = (BibtexEntry) c.iterator().next();

		assertEquals("1234567890123456789", (String) e.getField("isbn"));
		assertEquals("1234567890123456789", (String) e.getField("isbn2"));
		assertEquals("1234", (String) e.getField("small"));
	}

	public void testBigNumbers2() throws IOException {

		ParserResult result = BibtexParser.parse(new StringReader(""
			+ "@string{bourdieu = {Bourdieu, Pierre}}"
			+ "@book{bourdieu-2002-questions-sociologie, " + "	Address = {Paris},"
			+ "	Author = bourdieu," + "	Isbn = 2707318256," + "	Publisher = {Minuit},"
			+ "	Title = {Questions de sociologie}," + "	Year = 2002" + "}"));

		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = (BibtexEntry) c.iterator().next();

		assertEquals("bourdieu-2002-questions-sociologie", e.getCiteKey());
		assertEquals(BibtexEntryType.BOOK, e.getType());
		assertEquals("2707318256", (String) e.getField("isbn"));
		assertEquals("Paris", (String) e.getField("address"));
		assertEquals("Minuit", (String) e.getField("publisher"));
		assertEquals("Questions de sociologie", (String) e.getField("title"));
		assertEquals("#bourdieu#", (String) e.getField("author"));
		assertEquals("2002", (String) e.getField("year"));
	}

	public void testNewlineHandling() throws IOException {

		BibtexEntry e = BibtexParser.singleFromString("@article{canh05," +
				"a = {a\nb}," +
				"b = {a\n\nb}," +
				"c = {a\n \nb}," +
				"d = {a \n \n b},"
			+ "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
			+ "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
			+ "}");
		
		assertEquals("canh05", e.getCiteKey());
		assertEquals(BibtexEntryType.ARTICLE, e.getType());

		assertEquals("a b", (String)e.getField("a"));
		assertEquals("a\nb", (String)e.getField("b"));
		assertEquals("a b", (String)e.getField("c"));
		assertEquals("a b", (String)e.getField("d"));
		
		// I think the last \n is a bug in the parser...
		assertEquals("Hallo World this is\nnot \nan \n exercise . \n\n", (String) e.getField("title"));
		assertEquals("Hallo World this isnot an exercise . ", (String) e.getField("tabs"));
	}
}