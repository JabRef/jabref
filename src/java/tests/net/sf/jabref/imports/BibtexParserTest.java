package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.KeyCollisionException;
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
		for (int i = 0; i < o.length; i++){
			if (!e.getField(o[i].toString()).equals(e2.getField(o[i].toString()))){
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
			+ "isbn = 1234567890123456789,\n"
			+ "isbn2 = {1234567890123456789},\n"
			+ "small = 1234,\n"
			+ "}"));

		Collection c = result.getDatabase().getEntries();
		BibtexEntry e = (BibtexEntry) c.iterator().next();

		assertEquals("1234567890123456789", (String)e.getField("isbn"));
		assertEquals("1234567890123456789", (String)e.getField("isbn2"));
		assertEquals("1234", (String)e.getField("small"));
	}
	
	public void testBigNumbers2() throws IOException {

		
		ParserResult result = BibtexParser.parse(new StringReader("" +
				"@string{bourdieu = {Bourdieu, Pierre}}" +
				"@book{bourdieu-2002-questions-sociologie, " +
				"	Address = {Paris}," +
				"	Author = bourdieu," +
				"	Isbn = 2707318256," +
				"	Publisher = {Minuit}," +
				"	Title = {Questions de sociologie}," +
				"	Year = 2002" +
				"}"));

		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = (BibtexEntry) c.iterator().next();

		assertEquals("bourdieu-2002-questions-sociologie", e.getCiteKey());
		assertEquals(BibtexEntryType.BOOK, e.getType());
		assertEquals("2707318256", (String)e.getField("isbn"));
		assertEquals("Paris", (String)e.getField("address"));
		assertEquals("Minuit", (String)e.getField("publisher"));
		assertEquals("Questions de sociologie", (String)e.getField("Title"));
		assertEquals("#bourdieu#", (String)e.getField("Author"));
		assertEquals("2002", (String)e.getField("Year"));
	}

	public void testNewlineHandling() throws IOException {

		ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,"
			+ "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
			+ "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
			+ "}"));

		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());

		BibtexEntry e = (BibtexEntry) c.iterator().next();

		assertEquals("canh05", e.getCiteKey());
		assertEquals(BibtexEntryType.ARTICLE, e.getType());

		assertEquals("Hallo World this is not an exercise .", (String)e.getField("title"));
		assertEquals("Hallo World this is not an exercise .", (String)e.getField("tabs"));
	}
}