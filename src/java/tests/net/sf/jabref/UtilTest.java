package tests.net.sf.jabref;

import java.awt.Container;
import java.awt.Dialog;
import java.io.StringReader;
import java.util.HashSet;

import javax.swing.JDialog;
import javax.swing.JWindow;

import junit.framework.TestCase;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

public class UtilTest extends TestCase {

	public void testNCase() {
		assertEquals("", Util.nCase(""));
		assertEquals("Hello world", Util.nCase("Hello World"));
		assertEquals("A", Util.nCase("a"));
		assertEquals("Aa", Util.nCase("AA"));
	}

	public void testCheckName() {
		assertEquals("aa.bib", Util.checkName("aa"));
		assertEquals(".bib", Util.checkName(""));
		assertEquals("a.bib", Util.checkName("a.bib"));
		assertEquals("a.bib", Util.checkName("a"));
		assertEquals("a.bb.bib", Util.checkName("a.bb"));
	}

	public void testCreateNeutralId() {
		
		HashSet set = new HashSet();
		for (int i = 0; i < 10000; i++){
			String string = Util.createNeutralId();
			assertFalse(set.contains(string));
			set.add(string);
		}
		
	}

	public void testPlaceDialog() {
		Dialog d = new JDialog();
		d.setSize(50, 50);
		Container c = new JWindow();
		c.setBounds(100, 200, 100, 50);
		
		Util.placeDialog(d, c);
		assertEquals(125, d.getX());
		assertEquals(200, d.getY());
		
		// Test upper left corner
		c.setBounds(0,0,100,100);
		d.setSize(200, 200);
		
		Util.placeDialog(d, c);
		assertEquals(0, d.getX());
		assertEquals(0, d.getY());
	}

	public void testParseField() {
	
		assertEquals("", Util.parseField(""));
		
		// Three basic types (references, { } and " ")
		assertEquals("#hallo#", Util.parseField("hallo"));
		assertEquals("hallo", Util.parseField("{hallo}"));
		assertEquals("bye", Util.parseField("\"bye\""));
		
		// Concatenation
		assertEquals("longlonglonglong", Util.parseField("\"long\" # \"long\" # \"long\" # \"long\""));
		
		assertEquals("hallo#bye#", Util.parseField("{hallo} # bye"));
	}

	public void testShaveString() {
		
		assertEquals(null, Util.shaveString(null));
		assertEquals("", Util.shaveString(""));
		assertEquals("aaa", Util.shaveString("   aaa\t\t\n\r"));
		assertEquals("a", Util.shaveString("  {a}    "));
		assertEquals("a", Util.shaveString("  \"a\"    "));
		assertEquals("{a}", Util.shaveString("  {{a}}    "));
		assertEquals("{a}", Util.shaveString("  \"{a}\"    "));
		assertEquals("\"{a\"}", Util.shaveString("  \"{a\"}    "));
	}

	public void testCheckLegalKey() {

		assertEquals("AAAA", Util.checkLegalKey("AA AA"));
		assertEquals("SPECIALCHARS", Util.checkLegalKey("SPECIAL CHARS#{\\\"}~,^"));
		assertEquals("AeaeaAAA", Util.checkLegalKey("ÄäáÀÁÂ"));
		assertEquals("", Util.checkLegalKey("\n\t\r"));
	}

	public void testReplaceSpecialCharacters() {
		// Shouldn't German Ä be resolved to Ae
		assertEquals("AeaeaAAA", Util.replaceSpecialCharacters("ÄäáÀÁÂ"));
		assertEquals("Hallo Arger", Util.replaceSpecialCharacters("Hallo Arger"));
	}

	public void testJoin() {
		String[] s = "ab/cd/ed".split("/");
		assertEquals("ab\\cd\\ed", Util.join(s, "\\", 0, s.length));
		
		assertEquals("cd\\ed", Util.join(s, "\\", 1, s.length));
		
		assertEquals("ed", Util.join(s, "\\", 2, s.length));
		
		assertEquals("", Util.join(s, "\\", 3, s.length));
		
		assertEquals("", Util.join(new String[]{}, "\\", 0, 0));
	}
	
	public void testStripBrackets() {
		assertEquals("foo", Util.stripBrackets("[foo]"));
		assertEquals("[foo]", Util.stripBrackets("[[foo]]"));
		assertEquals("foo", Util.stripBrackets("foo]"));
		assertEquals("foo", Util.stripBrackets("[foo"));
		assertEquals("", Util.stripBrackets(""));
		assertEquals("", Util.stripBrackets("[]"));
		assertEquals("", Util.stripBrackets("["));
		assertEquals("", Util.stripBrackets("]"));
		assertEquals("f[]f", Util.stripBrackets("f[]f"));
	
		try {
			Util.stripBrackets(null);
			fail();
		} catch(NullPointerException npe){
			
		}
	}
	
	BibtexDatabase database;
	BibtexEntry entry;
	
	public void setUp(){
		
		StringReader reader = new StringReader(
				"@ARTICLE{HipKro03," + "\n" + 
				"  author = {Eric von Hippel and Georg von Krogh}," + "\n" + 
				"  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science}," + "\n" + 
				"  journal = {Organization Science}," + "\n" + 
				"  year = {2003}," + "\n" + 
				"  volume = {14}," + "\n" + 
				"  pages = {209--223}," + "\n" + 
				"  number = {2}," + "\n" + 
				"  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA}," + "\n" + 
				"  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n" + 
				"  issn = {1526-5455}," + "\n" + 
				"  publisher = {INFORMS}" + "\n" + 
				"}");

		BibtexParser parser = new BibtexParser(reader); 
		ParserResult result = null;
		try {
			result = parser.parse();
		} catch (Exception e){
			fail();
		}
		database = result.getDatabase();
		entry = database.getEntriesByKey("HipKro03")[0];
		
		assertNotNull(database);
		assertNotNull(entry);
	}

	public void testFieldAndFormat(){
		assertEquals("Eric von Hippel and Georg von Krogh", Util.getFieldAndFormat("[author]", entry, database));
		
		assertEquals("Eric von Hippel and Georg von Krogh", Util.getFieldAndFormat("author", entry, database));
		
		assertEquals(null, Util.getFieldAndFormat("[unknownkey]", entry, database));
		
		assertEquals("HipKro03", Util.getFieldAndFormat("[bibtexkey]", entry, database));
	}
	
	public void testExpandBrackets(){
				
		assertEquals("", Util.expandBrackets("", entry, database));
		
		assertEquals("dropped", Util.expandBrackets("drop[unknownkey]ped", entry, database));
		
		assertEquals("Eric von Hippel and Georg von Krogh", 
				Util.expandBrackets("[author]", entry, database));
		
		assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.", 
				Util.expandBrackets("[author] are two famous authors.", entry, database));

		assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.", 
				Util.expandBrackets("[author] are two famous authors.", entry, database));

		assertEquals("Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science in Organization Science.", 
				Util.expandBrackets("[author] have published [title] in [journal].", entry, database));
	}
}
