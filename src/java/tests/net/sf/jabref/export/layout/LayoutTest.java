package tests.net.sf.jabref.export.layout;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

public class LayoutTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		if (Globals.prefs == null){
			Globals.prefs = JabRefPreferences.getInstance();
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/* TEST DATA */
	public String t1BibtexString() {
		return "@article{canh05,\n"
			+ "  author = {This\nis\na\ntext},\n"
			+ "  title = {Effective work practices for floss development: A model and propositions},\n"
			+ "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},\n"
			+ "  year = {2005},\n" + "  owner = {oezbek},\n" + "  timestamp = {2006.05.29},\n"
			+ "  url = {http://james.howison.name/publications.html},\n" + "  abstract = {\\~{n}\n"
			+ "\\~n\n" + "\\'i\n" + "\\i\n" + "\\i}\n" + "}\n";
	}

	public BibtexEntry t1BibtexEntry() throws IOException {
		return bibtexString2BibtexEntry(t1BibtexString());
	}

	public static BibtexEntry bibtexString2BibtexEntry(String s) throws IOException {
		ParserResult result = BibtexParser.parse(new StringReader(s));
		Collection c = result.getDatabase().getEntries();
		assertEquals(1, c.size());
		return (BibtexEntry) c.iterator().next();
	}

	public String layout(String layoutFile, String entry) throws Exception {

		BibtexEntry be = bibtexString2BibtexEntry(entry);
		StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__", "\n"));
		Layout layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);
		StringBuffer sb = new StringBuffer();
		sb.append(layout.doLayout(be, null));

		return sb.toString();
	}

	public void testHTMLChar() throws Exception {
		String layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
			"@other{bla, author={This\nis\na\ntext}}");

		assertEquals("This is a text ", layoutText);

		// This fails!

		layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author}",
			"@other{bla, author={This\nis\na\ntext}}");

		assertEquals("This is a text", layoutText);

		layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
			"@other{bla, author={This\nis\na\n\ntext}}");

		assertEquals("This is a<p>text ", layoutText);

	}

	/**
	 * [ 1495181 ] Dotless i and tilde not handled in preview
	 * 
	 * @throws Exception
	 */
	public void testLayout() throws Exception {

		String layoutFile = "<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}</font>";

		StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__", "\n"));
		Layout layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);

		StringBuffer sb = new StringBuffer();
		sb.append(layout.doLayout(t1BibtexEntry(), null));
		String layoutText = sb.toString();

		assertEquals(
			"<font face=\"arial\"><BR><BR><b>Abstract: </b> &ntilde; &ntilde; &iacute; &#305; &#305;</font>",
			layoutText);

	}

}
