package tests.net.sf.jabref;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

public class BibtexDatabaseTest extends TestCase {

	/**
	 * Some basic test cases for resolving strings.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void testResolveStrings() throws FileNotFoundException, IOException{
		
		ParserResult result = BibtexParser.parse(new FileReader("src/tests/net/sf/jabref/util/twente.bib"));
		
		BibtexDatabase db = result.getDatabase();
		
		assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
		assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
		assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));
		
		// Strings that are not found return just the given string.
		assertEquals("#unknown#", db.resolveForStrings("#unknown#"));
		
	}
	
	
}
