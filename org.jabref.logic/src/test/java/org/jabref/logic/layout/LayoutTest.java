package org.jabref.logic.layout;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LayoutTest {

    private static ImportFormatPreferences importFormatPreferences;
    private LayoutFormatterPreferences layoutFormatterPreferences;

    /**
     * Initialize Preferences.
     */
    @BeforeEach
    public void setUp() {
        layoutFormatterPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
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
        ParserResult result = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor()).parse(new StringReader(s));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());
        return c.iterator().next();
    }

    public String layout(String layoutFile, String entry) throws IOException {
        BibEntry be = LayoutTest.bibtexString2BibtexEntry(entry);
        StringReader sr = new StringReader(layoutFile.replace("__NEWLINE__", "\n"));
        Layout layout = new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText();

        return layout.doLayout(be, null);
    }

    @Test
    public void testLayoutBibtextype() throws IOException {
        assertEquals("Unknown", layout("\\bibtextype", "@unknown{bla, author={This\nis\na\ntext}}"));
        assertEquals("Article", layout("\\bibtextype", "@article{bla, author={This\nis\na\ntext}}"));
        assertEquals("Misc", layout("\\bibtextype", "@misc{bla, author={This\nis\na\ntext}}"));
    }

    @Test
    public void testHTMLChar() throws IOException {
        String layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
                "@other{bla, author={This\nis\na\ntext}}");

        assertEquals("This is a text ", layoutText);

        layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author}",
                "@other{bla, author={This\nis\na\ntext}}");

        assertEquals("This is a text", layoutText);
    }

    @Test
    public void testPluginLoading() throws IOException {
        String layoutText = layout("\\begin{author}\\format[NameFormatter]{\\author}\\end{author}",
                "@other{bla, author={Joe Doe and Jane, Moon}}");

        assertEquals("Joe Doe, Moon Jane", layoutText);
    }

    @Test
    public void testHTMLCharDoubleLineBreak() throws IOException {
        String layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ",
                "@other{bla, author={This\nis\na\n\ntext}}");

        assertEquals("This is a text ", layoutText);
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

        assertEquals(
                "<font face=\"arial\"><BR><BR><b>Abstract: </b> &ntilde; &ntilde; &iacute; &imath; &imath;</font>",
                layoutText);
    }

    @Test
    // Test for http://discourse.jabref.org/t/the-wrapfilelinks-formatter/172 (the example in the help files)
    public void testWrapFileLinksLayout() throws IOException {
        when(layoutFormatterPreferences.getFileLinkPreferences()).thenReturn(
                new FileLinkPreferences(Collections.emptyList(), Collections.singletonList("src/test/resources/pdfs/")));

        String layoutText = layout("\\begin{file}\\format[WrapFileLinks(\\i. \\d (\\p))]{\\file}\\end{file}",
                "@other{bla, file={Test file:encrypted.pdf:PDF}}");

        assertEquals(
                "1. Test file (" + new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath() + ")",
                layoutText);
    }
}
