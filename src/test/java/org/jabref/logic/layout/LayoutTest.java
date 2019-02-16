package org.jabref.logic.layout;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

import org.jabref.logic.layout.format.FileLinkPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LayoutTest {

    private LayoutFormatterPreferences layoutFormatterPreferences;

    @BeforeEach
    void setUp() {
        layoutFormatterPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    private String layout(String layout, BibEntry entry) throws IOException {
        StringReader layoutStringReader = new StringReader(layout.replace("__NEWLINE__", "\n"));

        return new LayoutHelper(layoutStringReader, layoutFormatterPreferences)
                .getLayoutFromText()
                .doLayout(entry, null);
    }

    @Test
    void entryTypeForUnknown() throws IOException {
        BibEntry entry = new BibEntry(new CustomEntryType("unknown", "required", "optional")).withField("author", "test");

        assertEquals("Unknown", layout("\\bibtextype", entry));
    }

    @Test
    void entryTypeForArticle() throws IOException {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE).withField("author", "test");

        assertEquals("Article", layout("\\bibtextype", entry));
    }

    @Test
    void entryTypeForMisc() throws IOException {
        BibEntry entry = new BibEntry(BibtexEntryTypes.MISC).withField("author", "test");

        assertEquals("Misc", layout("\\bibtextype", entry));
    }

    @Test
    void HTMLChar() throws IOException {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE).withField("author", "This\nis\na\ntext");

        String actual = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author}", entry);

        assertEquals("This<br>is<br>a<br>text", actual);
    }

    @Test
    void HTMLCharWithDoubleLineBreak() throws IOException {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE).withField("author", "This\nis\na\n\ntext");

        String layoutText = layout("\\begin{author}\\format[HTMLChars]{\\author}\\end{author} ", entry);

        assertEquals("This<br>is<br>a<p>text ", layoutText);
    }

    @Test
    void nameFormatter() throws IOException {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE).withField("author", "Joe Doe and Jane, Moon");

        String layoutText = layout("\\begin{author}\\format[NameFormatter]{\\author}\\end{author}", entry);

        assertEquals("Joe Doe, Moon Jane", layoutText);
    }

    @Test
    void HTMLCharsWithDotlessIAndTiled() throws IOException {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE)
                .withField("abstract", "\\~{n} \\~n \\'i \\i \\i");

        String layoutText = layout(
                "<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>",
                entry);

        assertEquals(
                "<font face=\"arial\"><BR><BR><b>Abstract: </b> &ntilde; &ntilde; &iacute; &imath; &imath;</font>",
                layoutText);
    }

    @Test
        // Test for http://discourse.jabref.org/t/the-wrapfilelinks-formatter/172 (the example in the help files)
    void wrapFileLinksExpandFile() throws IOException {
        when(layoutFormatterPreferences.getFileLinkPreferences()).thenReturn(
                new FileLinkPreferences(Collections.emptyList(), Collections.singletonList("src/test/resources/pdfs/")));
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE);
        entry.addFile(new LinkedFile("Test file", "encrypted.pdf", "PDF"));

        String layoutText = layout("\\begin{file}\\format[WrapFileLinks(\\i. \\d (\\p))]{\\file}\\end{file}", entry);

        assertEquals(
                "1. Test file (" + new File("src/test/resources/pdfs/encrypted.pdf").getCanonicalPath() + ")",
                layoutText);
    }

    @Test
    void expandCommandIfTerminatedByMinus() throws IOException {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE).withField("edition", "2");

        String layoutText = layout("\\edition-th ed.-", entry);

        assertEquals("2-th ed.-", layoutText);
    }
}
