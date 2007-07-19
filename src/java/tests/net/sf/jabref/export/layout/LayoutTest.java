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

	/**
	 * Initialize Preferences.
	 */
	protected void setUp() throws Exception {
		super.setUp();
		if (Globals.prefs == null) {
			Globals.prefs = JabRefPreferences.getInstance();
		}
	}

	/**
	 * Return Test data.
	 */
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
		Collection<BibtexEntry> c = result.getDatabase().getEntries();
		assertEquals(1, c.size());
		return c.iterator().next();
	}

	public String layout(String layoutFile, String entry) throws Exception {

		BibtexEntry be = bibtexString2BibtexEntry(entry);
		StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__", "\n"));
		Layout layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);
		StringBuffer sb = new StringBuffer();
		sb.append(layout.doLayout(be, null));

		return sb.toString();
	}

	public void testLayoutBibtextype() throws Exception {
		assertEquals("Other", layout("\\bibtextype", "@other{bla, author={This\nis\na\ntext}}"));
		assertEquals("Article", layout("\\bibtextype", "@article{bla, author={This\nis\na\ntext}}"));
		assertEquals("Misc", layout("\\bibtextype", "@misc{bla, author={This\nis\na\ntext}}"));
	}

	public void testHTMLChar() throws Exception {
		String layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
			"@other{bla, author={This\nis\na\ntext}}");

		assertEquals("This is a text ", layoutText);

		layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author}",
			"@other{bla, author={This\nis\na\ntext}}");

		assertEquals("This is a text", layoutText);

		layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
			"@other{bla, author={This\nis\na\n\ntext}}");

		assertEquals("This is a<p>text ", layoutText);
	}
	
	public void testPluginLoading() throws Exception {
		String layoutText = layout("\\begin{author}\\format[NameFormatter]{\\author}\\end{author}",
			"@other{bla, author={Joe Doe and Jane, Moon}}");

		assertEquals("Joe Doe, Moon Jane", layoutText);
	}

	/**
	 * [ 1495181 ] Dotless i and tilde not handled in preview
	 * 
	 * @throws Exception
	 */
	public void testLayout() throws Exception {

		String layoutText = layout(
			"<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>",
			t1BibtexString());

		assertEquals(
			"<font face=\"arial\"><BR><BR><b>Abstract: </b> &ntilde; &ntilde; &iacute; &#305; &#305;</font>",
			layoutText);
	}
}
