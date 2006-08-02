package tests.net.sf.jabref;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;

import junit.framework.TestCase;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

public class UtilTest extends TestCase {

	public void testBool() {
		// cannot be tested
	}

	public void testPr() {
		// cannot be tested
	}

	public void testPr_() {
		// cannot be tested		
	}

	public void testNCase() {
		fail("Not yet implemented");
	}

	public void testCheckName() {
		fail("Not yet implemented");
	}

	public void testCreateNeutralId() {
		fail("Not yet implemented");
	}

	public void testPlaceDialog() {
		fail("Not yet implemented");
	}

	public void testParseField() {
		fail("Not yet implemented");
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
		fail("Not yet implemented");
	}

	public void testReplaceSpecialCharacters() {
		fail("Not yet implemented");
	}

	public void test_wrap2() {
		fail("Not yet implemented");
	}

	public void testWrap2() {
		fail("Not yet implemented");
	}

	public void test__wrap2() {
		fail("Not yet implemented");
	}

	public void testFindDeliminatedWordsInField() {
		fail("Not yet implemented");
	}

	public void testFindAllWordsInField() {
		fail("Not yet implemented");
	}

	public void testStringArrayToDelimited() {
		fail("Not yet implemented");
	}

	public void testDelimToStringArray() {
		fail("Not yet implemented");
	}

	public void testOpenExternalViewer() {
		fail("Not yet implemented");
	}

	public void testOpenFileOnWindows() {
		fail("Not yet implemented");
	}

	public void testOpenExternalFileAnyFormat() {
		fail("Not yet implemented");
	}

	public void testFindPdf() {
		fail("Not yet implemented");
	}

	public void testFindFile() {
		// -> Tested in UtilFindFileTest
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
	
	public void testExpandFilename() {
		fail("Not yet implemented");
	}

	public void testIsDuplicate() {
		fail("Not yet implemented");
	}

	public void testContainsDuplicate() {
		fail("Not yet implemented");
	}

	public void testCompareEntriesStrictly() {
		fail("Not yet implemented");
	}

	public void testSetAutomaticFieldsList() {
		fail("Not yet implemented");
	}

	public void testSetAutomaticFieldsBibtexEntry() {
		fail("Not yet implemented");
	}

	public void testCopyFile() {
		fail("Not yet implemented");
	}

	public void testPerformCompatibilityUpdate() {
		fail("Not yet implemented");
	}

	public void testGetCorrectFileName() {
		fail("Not yet implemented");
	}

	public void testQuoteForHTML() {
		fail("Not yet implemented");
	}

	public void testQuoteStringStringChar() {
		fail("Not yet implemented");
	}

	public void testQuoteStringStringCharInt() {
		fail("Not yet implemented");
	}

	public void testUnquote() {
		fail("Not yet implemented");
	}

	public void testQuoteMeta() {
		fail("Not yet implemented");
	}

	public void testSortWordsAndRemoveDuplicates() {
		fail("Not yet implemented");
	}

	public void testWarnAssignmentSideEffects() {
		fail("Not yet implemented");
	}

	public void testPutBracesAroundCapitals() {
		fail("Not yet implemented");
	}

	public void testRemoveBracesAroundCapitals() {
		fail("Not yet implemented");
	}

	public void testRemoveSingleBracesAroundCapitals() {
		fail("Not yet implemented");
	}

	public void testGetFileFilterForField() {
		fail("Not yet implemented");
	}

	public void testShowQuickErrorDialog() {
		fail("Not yet implemented");
	}

	public void testWrapHTML() {
		fail("Not yet implemented");
	}

	public void testEasyDateFormat() {
		fail("Not yet implemented");
	}

	public void testEasyDateFormatDate() {
		fail("Not yet implemented");
	}

	public void testMarkEntry() {
		fail("Not yet implemented");
	}

	public void testUnmarkEntry() {
		fail("Not yet implemented");
	}

	public void testIsMarked() {
		fail("Not yet implemented");
	}

	public void testFindEncodingsForString() {
		fail("Not yet implemented");
	}

}
