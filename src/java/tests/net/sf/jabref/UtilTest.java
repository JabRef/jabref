package tests.net.sf.jabref;

import java.awt.Container;
import java.awt.Dialog;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import javax.swing.JDialog;
import javax.swing.JWindow;

import junit.framework.TestCase;
import net.sf.jabref.*;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

public class UtilTest extends TestCase {

	public void testNCase() {
		assertEquals("", Util.nCase(""));
		assertEquals("Hello world", Util.nCase("Hello World"));
		assertEquals("A", Util.nCase("a"));
		assertEquals("Aa", Util.nCase("AA"));
	}

	public void testGetPublicationDate(){
	
		assertEquals("2003-02", Util.getPublicationDate(BibtexParser
			.singleFromString("@ARTICLE{HipKro03, year = {2003}, month = #FEB# }")));
		
		assertEquals("2003-03", Util.getPublicationDate(BibtexParser
			.singleFromString("@ARTICLE{HipKro03, year = {2003}, month = 3 }")));
		
		assertEquals("2003", Util.getPublicationDate(BibtexParser
			.singleFromString("@ARTICLE{HipKro03, year = {2003}}")));
		
		assertEquals(null, Util.getPublicationDate(BibtexParser
			.singleFromString("@ARTICLE{HipKro03, month = 3 }")));
		
		assertEquals(null, Util.getPublicationDate(BibtexParser
			.singleFromString("@ARTICLE{HipKro03, author={bla}}")));

		assertEquals("2003-12", Util.getPublicationDate(BibtexParser
			.singleFromString("@ARTICLE{HipKro03, year = {03}, month = #DEC# }")));
		
	}

	public void testCheckName() {
		assertEquals("aa.bib", Util.checkName("aa"));
		assertEquals(".bib", Util.checkName(""));
		assertEquals("a.bib", Util.checkName("a.bib"));
		assertEquals("a.bib", Util.checkName("a"));
		assertEquals("a.bb.bib", Util.checkName("a.bb"));
	}

	public void testCreateNeutralId() {
		
		HashSet<String> set = new HashSet<String>();
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
		assertEquals("AeaeaAAA", Util.checkLegalKey("������"));
		assertEquals("", Util.checkLegalKey("\n\t\r"));
	}

	public void testReplaceSpecialCharacters() {
		// Shouldn't German � be resolved to Ae
		assertEquals("AeaeaAAA", Util.replaceSpecialCharacters("������"));
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

	public void testParseMethodCalls(){
		
		assertEquals(1, Util.parseMethodsCalls("bla").size());
		assertEquals("bla", ((Util.parseMethodsCalls("bla").get(0)))[0]);
		
		assertEquals(1, Util.parseMethodsCalls("bla,").size());
		assertEquals("bla", ((Util.parseMethodsCalls("bla,").get(0)))[0]);

		assertEquals(1, Util.parseMethodsCalls("_bla.bla.blub,").size());
		assertEquals("_bla.bla.blub", ((Util.parseMethodsCalls("_bla.bla.blub,").get(0)))[0]);

		
		assertEquals(2, Util.parseMethodsCalls("bla,foo").size());
		assertEquals("bla", ((Util.parseMethodsCalls("bla,foo").get(0)))[0]);
		assertEquals("foo", ((Util.parseMethodsCalls("bla,foo").get(1)))[0]);
		
		assertEquals(2, Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").size());
		assertEquals("bla", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)))[0]);
		assertEquals("foo", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)))[0]);
		assertEquals("test", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)))[1]);
		assertEquals("fark", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)))[1]);
		
		assertEquals(2, Util.parseMethodsCalls("bla(test),foo(fark)").size());
		assertEquals("bla", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(0)))[0]);
		assertEquals("foo", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(1)))[0]);
		assertEquals("test", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(0)))[1]);
		assertEquals("fark", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(1)))[1]);
	}
	
	
	public void testFieldAndFormat(){
		assertEquals("Eric von Hippel and Georg von Krogh", Util.getFieldAndFormat("[author]", entry, database));
		
		assertEquals("Eric von Hippel and Georg von Krogh", Util.getFieldAndFormat("author", entry, database));
		
		assertEquals(null, Util.getFieldAndFormat("[unknownkey]", entry, database));
		
		assertEquals(null, Util.getFieldAndFormat("[:]", entry, database));
		
		assertEquals(null, Util.getFieldAndFormat("[:lower]", entry, database));
		
		assertEquals("eric von hippel and georg von krogh", Util.getFieldAndFormat("[author:lower]", entry, database));
		
		assertEquals("HipKro03", Util.getFieldAndFormat("[bibtexkey]", entry, database));
		
		assertEquals("HipKro03", Util.getFieldAndFormat("[bibtexkey:]", entry, database));
	}
	
	public void testUserFieldAndFormat(){
	
		String[] names = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATER_KEY);
		if (names == null)
			names = new String[]{};
		
		String[] formats = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATTER_VALUE);
		if (formats == null)
			formats = new String[]{};
		
		try {
		
			List<String> f = new LinkedList<String>(Arrays.asList(formats));
			List<String> n = new LinkedList<String>(Arrays.asList(names));
			
			n.add("testMe123454321");
			f.add("*@*@test");

			String[] newNames = n.toArray(new String[n.size()]);
			String[] newFormats = f.toArray(new String[f.size()]);
			
			Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATER_KEY, newNames);
			Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATTER_VALUE, newFormats);
			
			assertEquals("testtest", Util.getFieldAndFormat("[author:testMe123454321]", entry, database));
		
		} finally {
			Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATER_KEY, names);
			Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATTER_VALUE, formats);
		}
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

    public void testSanitizeUrl() {
    
            assertEquals("http://www.vg.no", Util.sanitizeUrl("http://www.vg.no"));
            assertEquals("http://www.vg.no/fil%20e.html",
                    Util.sanitizeUrl("http://www.vg.no/fil e.html"));
            assertEquals("http://www.vg.no/fil%20e.html",
                    Util.sanitizeUrl("http://www.vg.no/fil%20e.html"));
            assertEquals("www.vg.no/fil%20e.html",
                    Util.sanitizeUrl("www.vg.no/fil%20e.html"));

            assertEquals("www.vg.no/fil%20e.html",
                Util.sanitizeUrl("\\url{www.vg.no/fil%20e.html}"));
            
            /**
             * DOI Test cases
             */
            assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("10.1109/VLHCC.2004.20"));
            assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("doi://10.1109/VLHCC.2004.20"));
            assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("doi:/10.1109/VLHCC.2004.20"));
            assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("doi:10.1109/VLHCC.2004.20"));
    
            /**
             * Additional testcases provided by Hannes Restel and Micha Beckmann.
             */
            assertEquals("ftp://www.vg.no", Util.sanitizeUrl("ftp://www.vg.no"));
            assertEquals("file://doof.txt", Util.sanitizeUrl("file://doof.txt"));            
            assertEquals("file:///", Util.sanitizeUrl("file:///"));
            assertEquals("/src/doof.txt", Util.sanitizeUrl("/src/doof.txt"));
            assertEquals("/", Util.sanitizeUrl("/"));
            assertEquals("/home/user/example.txt", Util.sanitizeUrl("/home/user/example.txt"));
    }

    public void test2to4DigitsYear(){
    	assertEquals("1990", Util.toFourDigitYear("1990"));
    	assertEquals("190", Util.toFourDigitYear("190"));
    	assertEquals("1990", Util.toFourDigitYear("90", 1990));
    	assertEquals("1990", Util.toFourDigitYear("90", 1991));
    	assertEquals("2020", Util.toFourDigitYear("20", 1990));
    	assertEquals("1921", Util.toFourDigitYear("21", 1990));
    	assertEquals("1922", Util.toFourDigitYear("22", 1990));
    	assertEquals("2022", Util.toFourDigitYear("22", 1992));
    	assertEquals("1999", Util.toFourDigitYear("99", 2001));
    	assertEquals("1931", Util.toFourDigitYear("1931", 2001));
    	assertEquals("2031", Util.toFourDigitYear("31", 2001));
    	assertEquals("1932", Util.toFourDigitYear("32", 2001));
    	assertEquals("1944", Util.toFourDigitYear("44", 2001));
    	assertEquals("2011", Util.toFourDigitYear("11", 2001));
    	
    	int thisYear = Calendar.getInstance().get(Calendar.YEAR);
    	int d2 = thisYear % 100;

    	NumberFormat f = new DecimalFormat("00");
    	
    	for (int i = 0; i <= 30; i++){
    		assertTrue("" + i, thisYear <= Integer.parseInt(Util.toFourDigitYear(f.format((d2 + i) % 100))));
    	}
    	for (int i = 0; i < 70; i++){
    		assertTrue("" + i, thisYear >= Integer.parseInt(Util.toFourDigitYear(f.format((d2 - i + 100) % 100))));
    	}
    }
    
    public void testToMonthNumber(){

    	assertEquals(0, Util.getMonthNumber("jan"));
    	assertEquals(1, Util.getMonthNumber("feb"));
    	assertEquals(2, Util.getMonthNumber("mar"));
    	assertEquals(3, Util.getMonthNumber("apr"));
    	assertEquals(4, Util.getMonthNumber("may"));
    	assertEquals(5, Util.getMonthNumber("jun"));
    	assertEquals(6, Util.getMonthNumber("jul"));
    	assertEquals(7, Util.getMonthNumber("aug"));
    	assertEquals(8, Util.getMonthNumber("sep"));
    	assertEquals(9, Util.getMonthNumber("oct"));
    	assertEquals(10,Util.getMonthNumber("nov"));
    	assertEquals(11,Util.getMonthNumber("dec"));
    	
    	assertEquals(0, Util.getMonthNumber("#jan#"));
    	assertEquals(1, Util.getMonthNumber("#feb#"));
    	assertEquals(2, Util.getMonthNumber("#mar#"));
    	assertEquals(3, Util.getMonthNumber("#apr#"));
    	assertEquals(4, Util.getMonthNumber("#may#"));
    	assertEquals(5, Util.getMonthNumber("#jun#"));
    	assertEquals(6, Util.getMonthNumber("#jul#"));
    	assertEquals(7, Util.getMonthNumber("#aug#"));
    	assertEquals(8, Util.getMonthNumber("#sep#"));
    	assertEquals(9, Util.getMonthNumber("#oct#"));
    	assertEquals(10,Util.getMonthNumber("#nov#"));
    	assertEquals(11,Util.getMonthNumber("#dec#"));
    	
    	assertEquals(0, Util.getMonthNumber("January"));
    	assertEquals(1, Util.getMonthNumber("February"));
    	assertEquals(2, Util.getMonthNumber("March"));
    	assertEquals(3, Util.getMonthNumber("April"));
    	assertEquals(4, Util.getMonthNumber("May"));
    	assertEquals(5, Util.getMonthNumber("June"));
    	assertEquals(6, Util.getMonthNumber("July"));
    	assertEquals(7, Util.getMonthNumber("August"));
    	assertEquals(8, Util.getMonthNumber("September"));
    	assertEquals(9, Util.getMonthNumber("October"));
    	assertEquals(10,Util.getMonthNumber("November"));
    	assertEquals(11,Util.getMonthNumber("Decembre"));

    	assertEquals(0, Util.getMonthNumber("01"));
    	assertEquals(1, Util.getMonthNumber("02"));
    	assertEquals(2, Util.getMonthNumber("03"));
    	assertEquals(3, Util.getMonthNumber("04"));
    	assertEquals(4, Util.getMonthNumber("05"));
    	assertEquals(5, Util.getMonthNumber("06"));
    	assertEquals(6, Util.getMonthNumber("07"));
    	assertEquals(7, Util.getMonthNumber("08"));
    	assertEquals(8, Util.getMonthNumber("09"));
    	assertEquals(9, Util.getMonthNumber("10"));
    	
    	assertEquals(0, Util.getMonthNumber("1"));
    	assertEquals(1, Util.getMonthNumber("2"));
    	assertEquals(2, Util.getMonthNumber("3"));
    	assertEquals(3, Util.getMonthNumber("4"));
    	assertEquals(4, Util.getMonthNumber("5"));
    	assertEquals(5, Util.getMonthNumber("6"));
    	assertEquals(6, Util.getMonthNumber("7"));
    	assertEquals(7, Util.getMonthNumber("8"));
    	assertEquals(8, Util.getMonthNumber("9"));
    	    	
    	assertEquals(10,Util.getMonthNumber("11"));
    	assertEquals(11,Util.getMonthNumber("12"));

    	assertEquals(-1,Util.getMonthNumber(";lkjasdf"));
    	assertEquals(-1,Util.getMonthNumber("3.2"));
    	assertEquals(-1,Util.getMonthNumber("#test#"));
    	assertEquals(-1,Util.getMonthNumber(""));
    }
    
    public void testToUpperCharFirst(){
    	
    	assertEquals("", Util.toUpperFirstLetter(""));
    	assertEquals("A", Util.toUpperFirstLetter("a"));
    	assertEquals("A", Util.toUpperFirstLetter("A"));
    	assertEquals("An", Util.toUpperFirstLetter("an"));
    	assertEquals("AN", Util.toUpperFirstLetter("AN"));
    	assertEquals("TestTest", Util.toUpperFirstLetter("testTest"));
        
    }
}
