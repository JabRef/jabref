package tests.net.sf.jabref.export.layout.format;

import junit.framework.TestCase;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.NoSpaceBetweenAbbreviations;

public class NoSpaceBetweenAbbreviationsTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFormat() {
		LayoutFormatter f = new NoSpaceBetweenAbbreviations();
		assertEquals("", f.format(""));
		assertEquals("John Meier", f.format("John Meier"));
		assertEquals("J.F. Kennedy", f.format("J. F. Kennedy"));
		assertEquals("J.R.R. Tolkien", f.format("J. R. R. Tolkien"));
		assertEquals("J.R.R. Tolkien and J.F. Kennedy", f.format("J. R. R. Tolkien and J. F. Kennedy"));
	}

}
