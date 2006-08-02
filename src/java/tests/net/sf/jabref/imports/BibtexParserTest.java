package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

public class BibtexParserTest extends TestCase {
	
	public void testParseReader() throws IOException {
		
		ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test}}"));
		
		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());
		
		BibtexEntry e = (BibtexEntry)c.iterator().next();
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
		} catch(NullPointerException npe) {
			
		}
	}

	public void testIsRecognizedFormat() throws IOException {
		assertTrue(BibtexParser.isRecognizedFormat(new StringReader(
				"This file was created with JabRef 2.1 beta 2." + "\n" +
				"Encoding: Cp1252" + "\n" +
				"" + "\n" +
				"@INPROCEEDINGS{CroAnnHow05," + "\n" +
				"  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n" +
				"  title = {Effective work practices for floss development: A model and propositions}," + "\n" +
				"  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n" +
				"  year = {2005}," + "\n" +
				"  owner = {oezbek}," + "\n" +
				"  timestamp = {2006.05.29}," + "\n" +
				"  url = {http://james.howison.name/publications.html}" + "\n" +
				"}))")));
		
		assertTrue(BibtexParser.isRecognizedFormat(new StringReader(
				"This file was created with JabRef 2.1 beta 2." + "\n" +
				"Encoding: Cp1252" + "\n")));
		
		assertTrue(BibtexParser.isRecognizedFormat(new StringReader(
				"@INPROCEEDINGS{CroAnnHow05," + "\n" +
				"  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n" +
				"  title = {Effective work practices for floss development: A model and propositions}," + "\n" +
				"  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n" +
				"  year = {2005}," + "\n" +
				"  owner = {oezbek}," + "\n" +
				"  timestamp = {2006.05.29}," + "\n" +
				"  url = {http://james.howison.name/publications.html}" + "\n" +
				"}))")));
		
		assertFalse(BibtexParser.isRecognizedFormat(new StringReader(
				"  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n" +
				"  title = {Effective work practices for floss development: A model and propositions}," + "\n" +
				"  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n" +
				"  year = {2005}," + "\n" +
				"  owner = {oezbek}," + "\n" +
				"  timestamp = {2006.05.29}," + "\n" +
				"  url = {http://james.howison.name/publications.html}" + "\n" +
				"}))")));
		
		assertFalse(BibtexParser.isRecognizedFormat(new StringReader(
				"This was created with JabRef 2.1 beta 2." + "\n" +
				"Encoding: Cp1252" + "\n")));
	}

	public void testParse() throws IOException {
		
		// Test Standard parsing
		BibtexParser parser = new BibtexParser(new StringReader("@article{test,author={Ed von Test}}"));
		ParserResult result = parser.parse();
		
		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());
		
		BibtexEntry e = (BibtexEntry)c.iterator().next();
		assertEquals("test", e.getCiteKey());
		assertEquals(2, e.getAllFields().length);
		Object[] o = e.getAllFields();
		assertTrue(o[0].toString().equals("author") || o[1].toString().equals("author"));
		assertEquals("Ed von Test", e.getField("author"));
		
		// Calling parse again will return the same result
		assertEquals(result, parser.parse());
	}

}
