package tests.net.sf.jabref.export.layout.format;

import junit.framework.TestCase;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.AuthorOrgSci;
import net.sf.jabref.export.layout.format.CompositeFormat;
import net.sf.jabref.export.layout.format.NoSpaceBetweenAbbreviations;

public class CompositeFormatTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testComposite() {

		{
			LayoutFormatter f = new CompositeFormat();
			assertEquals("No Change", f.format("No Change"));
		}
		{ 
			LayoutFormatter f = new CompositeFormat(new LayoutFormatter[]{new LayoutFormatter(){

				public String format(String fieldText) {
					return fieldText + fieldText;
				}
				
			}, new LayoutFormatter(){

				public String format(String fieldText) {
					return "A" + fieldText;
				}
				
			}, new LayoutFormatter(){

				public String format(String fieldText) {
					return "B" + fieldText;
				}
				
			}});
			
			assertEquals("BAff", f.format("f"));
		}
		
		{
			LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(),
				new NoSpaceBetweenAbbreviations());
			LayoutFormatter first = new AuthorOrgSci();
			LayoutFormatter second = new NoSpaceBetweenAbbreviations();
			
			assertEquals(second.format(first.format("John Flynn and Sabine Gartska")), f.format("John Flynn and Sabine Gartska"));
			assertEquals(second.format(first.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee")), f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
		}
	}

}
