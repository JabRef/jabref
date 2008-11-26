package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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

		Collection<BibtexEntry> c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = c.iterator().next();
		assertEquals("test", e.getCiteKey());
		assertEquals(2, e.getAllFields().size());
		Set<String> o = e.getAllFields();
		assertTrue(o.contains("author"));
		assertEquals("Ed von Test", e.getField("author"));
	}

	public void testBibtexParser() {
		try {
			new BibtexParser(null);
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
			Collection<BibtexEntry> c = BibtexParser.fromString("@article{test,author={Ed von Test}}");
			assertEquals(1, c.size());

			BibtexEntry e = c.iterator().next();
			assertEquals("test", e.getCiteKey());
			assertEquals(2, e.getAllFields().size());
			assertTrue(e.getAllFields().contains("author"));
			assertEquals("Ed von Test", e.getField("author"));
		}
		{ // Empty String
			Collection<BibtexEntry> c = BibtexParser.fromString("");
			assertEquals(0, c.size());

		}
		{ // Error
			Collection<BibtexEntry> c = BibtexParser.fromString("@@article@@{{{{{{}");
			assertEquals(null, c);
		}

	}

	public void testFromSingle2() {
		/**
		 * More
		 */
		Collection<BibtexEntry> c = BibtexParser.fromString("@article{canh05,"
			+ "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n"
			+ "@inProceedings{foo," + "  author={Norton Bar}}");

		assertEquals(2, c.size());

		Iterator<BibtexEntry> i = c.iterator();
		BibtexEntry a = i.next();
		BibtexEntry b = i.next();

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

		Collection<BibtexEntry> c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = c.iterator().next();
		assertEquals("test", e.getCiteKey());
		assertEquals(2, e.getAllFields().size());
		assertTrue(e.getAllFields().contains("author"));
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

		Collection<BibtexEntry> c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e2 = c.iterator().next();

		assertNotSame(e.getId(), e2.getId());

		for (String field : e.getAllFields()){
			if (!e.getField(field.toString()).equals(e2.getField(field.toString()))) {
				fail("e and e2 differ in field " + field.toString());
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

		Collection<BibtexEntry> c = result.getDatabase().getEntries();
		BibtexEntry e = c.iterator().next();

		assertEquals("1234567890123456789", e.getField("isbn"));
		assertEquals("1234567890123456789", e.getField("isbn2"));
		assertEquals("1234", e.getField("small"));
	}

	public void testBigNumbers2() throws IOException {

		ParserResult result = BibtexParser.parse(new StringReader(""
			+ "@string{bourdieu = {Bourdieu, Pierre}}"
			+ "@book{bourdieu-2002-questions-sociologie, " + "	Address = {Paris},"
			+ "	Author = bourdieu," + "	Isbn = 2707318256," + "	Publisher = {Minuit},"
			+ "	Title = {Questions de sociologie}," + "	Year = 2002" + "}"));

		Collection<BibtexEntry> c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = c.iterator().next();

		assertEquals("bourdieu-2002-questions-sociologie", e.getCiteKey());
		assertEquals(BibtexEntryType.BOOK, e.getType());
		assertEquals("2707318256", e.getField("isbn"));
		assertEquals("Paris", e.getField("address"));
		assertEquals("Minuit", e.getField("publisher"));
		assertEquals("Questions de sociologie", e.getField("title"));
		assertEquals("#bourdieu#", e.getField("author"));
		assertEquals("2002", e.getField("year"));
	}

	public void testNewlineHandling() throws IOException {

		BibtexEntry e = BibtexParser.singleFromString("@article{canh05," +
				"a = {a\nb}," +
				"b = {a\n\nb}," +
				"c = {a\n \nb}," +
				"d = {a \n \n b},"
			+ "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
			+ "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
			+ "file = {Bemerkung:H:\\bla\\ups  sala.pdf:PDF}, \n"
			+ "}");
		
		assertEquals("canh05", e.getCiteKey());
		assertEquals(BibtexEntryType.ARTICLE, e.getType());

		assertEquals("a b", e.getField("a"));
		assertEquals("a\nb", e.getField("b"));
		assertEquals("a b", e.getField("c"));
		assertEquals("a b", e.getField("d"));
		
		// I think the last \n is a bug in the parser...
		assertEquals("Hallo World this is\nnot \nan \n exercise . \n\n", e.getField("title"));
		assertEquals("Hallo World this isnot an exercise . ", e.getField("tabs"));
	}
	
	/**
	 * Test for [2022983]
	 * 
	 * @author Uwe Kuehn
	 * @author Andrei Haralevich
	 */
	public void testFileNaming(){
		BibtexEntry e = BibtexParser.singleFromString("@article{canh05," 
			+ "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
			+ "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
			+ "file = {Bemerkung:H:\\bla\\ups  sala.pdf:PDF}, \n"
			+ "}");
		
		assertEquals("Bemerkung:H:\\bla\\ups  sala.pdf:PDF", e.getField("file"));
	}
	
	/**
	 * Test for [2022983]
	 * 
	 * @author Uwe Kuehn
	 * @author Andrei Haralevich
	 */
	public void testFileNaming1(){
		BibtexEntry e = BibtexParser.singleFromString("@article{canh05," 
			+ "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
			+ "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
			+ "file = {Bemerkung:H:\\bla\\ups  \tsala.pdf:PDF}, \n"
			+ "}");
		
		assertEquals("Bemerkung:H:\\bla\\ups  sala.pdf:PDF", e.getField("file"));
	}
	
	/**
	 * Test for [2022983]
	 * 
	 * @author Uwe Kuehn
	 * @author Andrei Haralevich
	 */
	public void testFileNaming3(){
		BibtexEntry e = BibtexParser.singleFromString("@article{canh05," 
			+ "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
			+ "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
			+ "file = {Bemerkung:H:\\bla\\ups \n\tsala.pdf:PDF}, \n"
			+ "}");
		
		assertEquals("Bemerkung:H:\\bla\\ups  sala.pdf:PDF", e.getField("file"));
	}
}