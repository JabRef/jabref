package tests.net.sf.jabref.export.layout.format;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.ResolvePDF;
import tests.net.sf.jabref.FileBasedTestCase;

/**
 * Testing the PDF resolver.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public class ResolvePDFTest extends FileBasedTestCase {

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() {
		super.tearDown();
	}

	public void testFormat() throws URISyntaxException {
		LayoutFormatter pdf = new ResolvePDF();

		assertEquals("", pdf.format(""));
		
		/*
		 * Check one that will be found
		 */
		String result = pdf.format("Organization Science\\HipKro03 - Hello.pdf");
		assertTrue(result.startsWith("file:/"));
		
		assertTrue(result.endsWith("/Organization%20Science/HipKro03%20-%20Hello.pdf"));
		
		// Should not contain a backslash:
		assertEquals(-1, result.indexOf('\\'));
		
		assertTrue(new File(new URI(result)).exists());
		
		/*
		 * And one that is not to be found
		 */
		result = pdf.format("Organization Science/Does not exist.pdf");
		assertEquals("Organization Science/Does not exist.pdf", result);
	}
}
