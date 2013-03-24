package tests.net.sf.jabref.export.layout.format;

import junit.framework.TestCase;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.AuthorOrgSci;
import net.sf.jabref.export.layout.format.CompositeFormat;
import net.sf.jabref.export.layout.format.NoSpaceBetweenAbbreviations;

public class AuthorOrgSciTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testOrgSci(){
		LayoutFormatter f = new AuthorOrgSci();
		
		assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
		assertEquals("Garvin, D. A.", f.format("David A. Garvin"));
		assertEquals("Makridakis, S., S. C. Wheelwright, V. E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
		
	}
	public void testOrgSciPlusAbbreviation(){
		LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(), new NoSpaceBetweenAbbreviations());
		assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
		assertEquals("Garvin, D.A.", f.format("David A. Garvin"));
		assertEquals("Makridakis, S., S.C. Wheelwright, V.E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
	}
}
