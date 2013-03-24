package tests.net.sf.jabref.export.layout.format;

import junit.framework.TestCase;
import net.sf.jabref.export.layout.format.plugin.NameFormat;

public class BibtexNameLayoutFormatterTest extends TestCase {

	public void testFormatStringStringBibtexEntry() {
		
		NameFormat l = new NameFormat();
		
		assertEquals("Doe", l.format("Joe Doe", "1@*@{ll}", null));		
		
		assertEquals("moremoremoremore", l.format("Joe Doe and Mary Jane and Bruce Bar and Arthur Kay", 
			"1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{last}@@*@*@more", null));
		
		assertEquals("Doe", l.format("Joe Doe", 
			"1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{last}@@*@*@more", null));
	
		assertEquals("JoeDoe and MaryJ", l.format("Joe Doe and Mary Jane", 
			"1@*@{ll}@@2@1..1@{ff}{ll}@2..2@ and {ff}{l}@@*@*@more", null));

		assertEquals("Doe, Joe and Jane, M. and Kamp, J.~A.", l.format("Joe Doe and Mary Jane and John Arthur van Kamp", 
			"1@*@{ll}, {ff}@@*@1@{ll}, {ff}@2..-1@ and {ll}, {f}.", null));
	
		assertEquals("Doe Joe and Jane, M. and Kamp, J.~A.", l.format("Joe Doe and Mary Jane and John Arthur van Kamp", 
			"1@*@{ll}, {ff}@@*@1@{ll} {ff}@2..-1@ and {ll}, {f}.", null));
		
	}
	
	public void testFormat() {

		NameFormat a = new NameFormat();

		// Empty case
		assertEquals("", a.format(""));

		String formatString = "1@1@{vv }{ll}{ ff}@@2@1@{vv }{ll}{ ff}@2@ and {vv }{ll}{, ff}@@*@1@{vv }{ll}{ ff}@2..-2@, {vv }{ll}{, ff}@-1@ and {vv }{ll}{, ff}";
		
		// Single Names
		assertEquals("Vandekamp Mary~Ann", a.format("Mary Ann Vandekamp", formatString, null));

		// Two names
		assertEquals("von Neumann John and Black~Brown, Peter", a
			.format("John von Neumann and Black Brown, Peter",formatString, null));

		// Three names
		assertEquals("von Neumann John, Smith, John and Black~Brown, Peter", a
			.format("von Neumann, John and Smith, John and Black Brown, Peter", formatString, null));

		assertEquals("von Neumann John, Smith, John and Black~Brown, Peter", a
			.format("John von Neumann and John Smith and Black Brown, Peter", formatString, null));
		
		// Four names
		assertEquals("von Neumann John, Smith, John, Vandekamp, Mary~Ann and Black~Brown, Peter", a
			.format("von Neumann, John and Smith, John and Vandekamp, Mary Ann and Black Brown, Peter", formatString, null));
	}
	
}
