package net.sf.jabref.logic.layout;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

<<<<<<< 37a4f29080b77845f0d4a227a527e2699e199026
import net.sf.jabref.logic.importer.ImportFormatPreferences;
=======
import net.sf.jabref.Globals;
>>>>>>> Moved some logic preference instantiation to JabRefPreferences
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class LayoutTest {

    private LayoutFormatterPreferences prefs;


    /**
     * Initialize Preferences.
     */
    @Before
    public void setUp() {
<<<<<<< 37a4f29080b77845f0d4a227a527e2699e199026
        prefs = LayoutFormatterPreferences.fromPreferences(JabRefPreferences.getInstance(),
                mock(JournalAbbreviationLoader.class));
=======
        Globals.prefs = JabRefPreferences.getInstance();
        prefs = JabRefPreferences.getInstance().getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class));
>>>>>>> Moved some logic preference instantiation to JabRefPreferences
    }

    /**
     * Return Test data.
     */
    public String t1BibtexString() {
        return "@article{canh05,\n" + "  author = {This\nis\na\ntext},\n"
                + "  title = {Effective work practices for floss development: A model and propositions},\n"
                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},\n" + "  year = {2005},\n"
                + "  owner = {oezbek},\n" + "  timestamp = {2006.05.29},\n"
                + "  url = {http://james.howison.name/publications.html},\n" + "  abstract = {\\~{n} \\~n "
                + "\\'i \\i \\i}\n" + "}\n";
    }

    public static BibEntry bibtexString2BibtexEntry(String s) throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(s),
<<<<<<< 37a4f29080b77845f0d4a227a527e2699e199026
                ImportFormatPreferences.fromPreferences(JabRefPreferences.getInstance()));
=======
                Globals.prefs.getImportFormatPreferences());
>>>>>>> Moved some logic preference instantiation to JabRefPreferences
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());
        return c.iterator().next();
    }

    public String layout(String layoutFile, String entry) throws IOException {

        BibEntry be = LayoutTest.bibtexString2BibtexEntry(entry);
        StringReader sr = new StringReader(layoutFile.replace("__NEWLINE__", "\n"));
        Layout layout = new LayoutHelper(sr, prefs)
                        .getLayoutFromText();

        return layout.doLayout(be, null);
    }

    @Test
    public void testLayoutBibtextype() throws IOException {
        Assert.assertEquals("Unknown", layout("\\bibtextype", "@unknown{bla, author={This\nis\na\ntext}}"));
        Assert.assertEquals("Article", layout("\\bibtextype", "@article{bla, author={This\nis\na\ntext}}"));
        Assert.assertEquals("Misc", layout("\\bibtextype", "@misc{bla, author={This\nis\na\ntext}}"));
    }

    @Test
    public void testHTMLChar() throws IOException {
        String layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
                "@other{bla, author={This\nis\na\ntext}}");

        Assert.assertEquals("This is a text ", layoutText);

        layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author}",
                "@other{bla, author={This\nis\na\ntext}}");

        Assert.assertEquals("This is a text", layoutText);
    }

    @Test
    @Ignore
    public void testHTMLCharDoubleLineBreak() throws IOException {
        String layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
                "@other{bla, author={This\nis\na\n\ntext}}");

        Assert.assertEquals("This is a<br>text ", layoutText);
    }

    @Test
    public void testPluginLoading() throws IOException {
        String layoutText = layout("\\begin{author}\\format[NameFormatter]{\\author}\\end{author}",
                "@other{bla, author={Joe Doe and Jane, Moon}}");

        Assert.assertEquals("Joe Doe, Moon Jane", layoutText);
    }

    /**
     * [ 1495181 ] Dotless i and tilde not handled in preview
     *
     * @throws Exception
     */
    @Test
    public void testLayout() throws IOException {

        String layoutText = layout(
                "<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>",
                t1BibtexString());

        Assert.assertEquals(
                "<font face=\"arial\"><BR><BR><b>Abstract: </b> &ntilde; &ntilde; &iacute; &imath; &imath;</font>",
                layoutText);
    }
}
