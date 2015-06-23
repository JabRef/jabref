package net.sf.jabref;

import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;

import java.awt.*;
import java.io.StringReader;
import java.util.*;
import java.util.List;

public class UtilTest {

    @Test
    public void testNCase() {
        Assert.assertEquals("", Util.nCase(""));
        Assert.assertEquals("Hello world", Util.nCase("Hello World"));
        Assert.assertEquals("A", Util.nCase("a"));
        Assert.assertEquals("Aa", Util.nCase("AA"));
    }

    @Test
    public void testGetPublicationDate() {

        Assert.assertEquals("2003-02", Util.getPublicationDate(BibtexParser
                .singleFromString("@ARTICLE{HipKro03, year = {2003}, month = #FEB# }")));

        Assert.assertEquals("2003-03", Util.getPublicationDate(BibtexParser
                .singleFromString("@ARTICLE{HipKro03, year = {2003}, month = 3 }")));

        Assert.assertEquals("2003", Util.getPublicationDate(BibtexParser
                .singleFromString("@ARTICLE{HipKro03, year = {2003}}")));

        Assert.assertEquals(null, Util.getPublicationDate(BibtexParser
                .singleFromString("@ARTICLE{HipKro03, month = 3 }")));

        Assert.assertEquals(null, Util.getPublicationDate(BibtexParser
                .singleFromString("@ARTICLE{HipKro03, author={bla}}")));

        Assert.assertEquals("2003-12", Util.getPublicationDate(BibtexParser
                .singleFromString("@ARTICLE{HipKro03, year = {03}, month = #DEC# }")));

    }

    @Test
    public void testMakeBibtexExtension() {
        Assert.assertEquals("aa.bib", Util.makeBibtexExtension("aa"));
        Assert.assertEquals(".bib", Util.makeBibtexExtension(""));
        Assert.assertEquals("a.bib", Util.makeBibtexExtension("a.bib"));
        Assert.assertEquals("a.bib", Util.makeBibtexExtension("a"));
        Assert.assertEquals("a.bb.bib", Util.makeBibtexExtension("a.bb"));
    }

    @Test
    @Ignore
    public void testPlaceDialog() {
        Dialog d = new JDialog();
        d.setSize(50, 50);
        Container c = new JWindow();
        c.setBounds(100, 200, 100, 50);

        Util.placeDialog(d, c);
        Assert.assertEquals(125, d.getX());
        Assert.assertEquals(200, d.getY());

        // Test upper left corner
        c.setBounds(0, 0, 100, 100);
        d.setSize(200, 200);

        Util.placeDialog(d, c);
        Assert.assertEquals(0, d.getX());
        Assert.assertEquals(0, d.getY());
    }

    @Test
    public void testParseField() {

        Assert.assertEquals("", Util.parseField(""));

        // Three basic types (references, { } and " ")
        Assert.assertEquals("#hallo#", Util.parseField("hallo"));
        Assert.assertEquals("hallo", Util.parseField("{hallo}"));
        Assert.assertEquals("bye", Util.parseField("\"bye\""));

        // Concatenation
        Assert.assertEquals("longlonglonglong", Util.parseField("\"long\" # \"long\" # \"long\" # \"long\""));

        Assert.assertEquals("hallo#bye#", Util.parseField("{hallo} # bye"));
    }

    @Test
    public void testShaveString() {

        Assert.assertEquals(null, Util.shaveString(null));
        Assert.assertEquals("", Util.shaveString(""));
        Assert.assertEquals("aaa", Util.shaveString("   aaa\t\t\n\r"));
        Assert.assertEquals("a", Util.shaveString("  {a}    "));
        Assert.assertEquals("a", Util.shaveString("  \"a\"    "));
        Assert.assertEquals("{a}", Util.shaveString("  {{a}}    "));
        Assert.assertEquals("{a}", Util.shaveString("  \"{a}\"    "));
        Assert.assertEquals("\"{a\"}", Util.shaveString("  \"{a\"}    "));
    }

    @Test
    @Ignore
    public void testCheckLegalKey() {

        Assert.assertEquals("AAAA", Util.checkLegalKey("AA AA"));
        Assert.assertEquals("SPECIALCHARS", Util.checkLegalKey("SPECIAL CHARS#{\\\"}~,^"));
        Assert.assertEquals("AeaeaAAA", Util.checkLegalKey("ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½"));
        Assert.assertEquals("", Util.checkLegalKey("\n\t\r"));
    }

    @Test
    @Ignore
    public void testReplaceSpecialCharacters() {
        // Shouldn't German ï¿½ be resolved to Ae
        Assert.assertEquals("AeaeaAAA", Util.replaceSpecialCharacters("ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½"));
        Assert.assertEquals("Hallo Arger", Util.replaceSpecialCharacters("Hallo Arger"));
    }

    @Test
    public void testJoin() {
        String[] s = "ab/cd/ed".split("/");
        Assert.assertEquals("ab\\cd\\ed", Util.join(s, "\\", 0, s.length));

        Assert.assertEquals("cd\\ed", Util.join(s, "\\", 1, s.length));

        Assert.assertEquals("ed", Util.join(s, "\\", 2, s.length));

        Assert.assertEquals("", Util.join(s, "\\", 3, s.length));

        Assert.assertEquals("", Util.join(new String[] {}, "\\", 0, 0));
    }

    @Test
    public void testStripBrackets() {
        Assert.assertEquals("foo", Util.stripBrackets("[foo]"));
        Assert.assertEquals("[foo]", Util.stripBrackets("[[foo]]"));
        Assert.assertEquals("foo", Util.stripBrackets("foo]"));
        Assert.assertEquals("foo", Util.stripBrackets("[foo"));
        Assert.assertEquals("", Util.stripBrackets(""));
        Assert.assertEquals("", Util.stripBrackets("[]"));
        Assert.assertEquals("", Util.stripBrackets("["));
        Assert.assertEquals("", Util.stripBrackets("]"));
        Assert.assertEquals("f[]f", Util.stripBrackets("f[]f"));

        try {
            Util.stripBrackets(null);
            Assert.fail();
        } catch (NullPointerException ignored) {

        }
    }


    BibtexDatabase database;
    BibtexEntry entry;


    @Before
    public void setUp() {

        StringReader reader = new StringReader(
                "@ARTICLE{HipKro03," + "\n" +
                        "  author = {Eric von Hippel and Georg von Krogh}," + "\n" +
                        "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science}," + "\n" +
                        "  journal = {Organization Science}," + "\n" +
                        "  year = {2003}," + "\n" +
                        "  volume = {14}," + "\n" +
                        "  pages = {209--223}," + "\n" +
                        "  number = {2}," + "\n" +
                        "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA}," + "\n" +
                        "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n" +
                        "  issn = {1526-5455}," + "\n" +
                        "  publisher = {INFORMS}" + "\n" +
                        "}"
                );

        BibtexParser parser = new BibtexParser(reader);
        ParserResult result = null;
        try {
            result = parser.parse();
        } catch (Exception e) {
            Assert.fail();
        }
        database = result.getDatabase();
        entry = database.getEntriesByKey("HipKro03")[0];

        Assert.assertNotNull(database);
        Assert.assertNotNull(entry);
    }

    @Test
    public void testParseMethodCalls() {

        Assert.assertEquals(1, Util.parseMethodsCalls("bla").size());
        Assert.assertEquals("bla", ((Util.parseMethodsCalls("bla").get(0)))[0]);

        Assert.assertEquals(1, Util.parseMethodsCalls("bla,").size());
        Assert.assertEquals("bla", ((Util.parseMethodsCalls("bla,").get(0)))[0]);

        Assert.assertEquals(1, Util.parseMethodsCalls("_bla.bla.blub,").size());
        Assert.assertEquals("_bla.bla.blub", ((Util.parseMethodsCalls("_bla.bla.blub,").get(0)))[0]);

        Assert.assertEquals(2, Util.parseMethodsCalls("bla,foo").size());
        Assert.assertEquals("bla", ((Util.parseMethodsCalls("bla,foo").get(0)))[0]);
        Assert.assertEquals("foo", ((Util.parseMethodsCalls("bla,foo").get(1)))[0]);

        Assert.assertEquals(2, Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").size());
        Assert.assertEquals("bla", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)))[0]);
        Assert.assertEquals("foo", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)))[0]);
        Assert.assertEquals("test", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)))[1]);
        Assert.assertEquals("fark", ((Util.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)))[1]);

        Assert.assertEquals(2, Util.parseMethodsCalls("bla(test),foo(fark)").size());
        Assert.assertEquals("bla", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(0)))[0]);
        Assert.assertEquals("foo", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(1)))[0]);
        Assert.assertEquals("test", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(0)))[1]);
        Assert.assertEquals("fark", ((Util.parseMethodsCalls("bla(test),foo(fark)").get(1)))[1]);
    }

    @Test
    @Ignore
    public void testFieldAndFormat() {
        Assert.assertEquals("Eric von Hippel and Georg von Krogh", Util.getFieldAndFormat("[author]", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh", Util.getFieldAndFormat("author", entry, database));

        Assert.assertEquals(null, Util.getFieldAndFormat("[unknownkey]", entry, database));

        Assert.assertEquals(null, Util.getFieldAndFormat("[:]", entry, database));

        Assert.assertEquals(null, Util.getFieldAndFormat("[:lower]", entry, database));

        Assert.assertEquals("eric von hippel and georg von krogh", Util.getFieldAndFormat("[author:lower]", entry, database));

        Assert.assertEquals("HipKro03", Util.getFieldAndFormat("[bibtexkey]", entry, database));

        Assert.assertEquals("HipKro03", Util.getFieldAndFormat("[bibtexkey:]", entry, database));
    }

    @Test
    @Ignore
    public void testUserFieldAndFormat() {

        String[] names = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATER_KEY);
        if (names == null) {
            names = new String[] {};
        }

        String[] formats = Globals.prefs.getStringArray(NameFormatterTab.NAME_FORMATTER_VALUE);
        if (formats == null) {
            formats = new String[] {};
        }

        try {

            List<String> f = new LinkedList<String>(Arrays.asList(formats));
            List<String> n = new LinkedList<String>(Arrays.asList(names));

            n.add("testMe123454321");
            f.add("*@*@test");

            String[] newNames = n.toArray(new String[n.size()]);
            String[] newFormats = f.toArray(new String[f.size()]);

            Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATER_KEY, newNames);
            Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATTER_VALUE, newFormats);

            Assert.assertEquals("testtest", Util.getFieldAndFormat("[author:testMe123454321]", entry, database));

        } finally {
            Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATER_KEY, names);
            Globals.prefs.putStringArray(NameFormatterTab.NAME_FORMATTER_VALUE, formats);
        }
    }

    @Test
    public void testExpandBrackets() {

        Assert.assertEquals("", Util.expandBrackets("", entry, database));

        Assert.assertEquals("dropped", Util.expandBrackets("drop[unknownkey]ped", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh",
                Util.expandBrackets("[author]", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                Util.expandBrackets("[author] are two famous authors.", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                Util.expandBrackets("[author] are two famous authors.", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                Util.expandBrackets("[author] have published [title] in [journal].", entry, database));
    }

    @Test
    public void testSanitizeUrl() {

        Assert.assertEquals("http://www.vg.no", Util.sanitizeUrl("http://www.vg.no"));
        Assert.assertEquals("http://www.vg.no/fil%20e.html",
                Util.sanitizeUrl("http://www.vg.no/fil e.html"));
        Assert.assertEquals("http://www.vg.no/fil%20e.html",
                Util.sanitizeUrl("http://www.vg.no/fil%20e.html"));
        Assert.assertEquals("www.vg.no/fil%20e.html",
                Util.sanitizeUrl("www.vg.no/fil%20e.html"));

        Assert.assertEquals("www.vg.no/fil%20e.html",
                Util.sanitizeUrl("\\url{www.vg.no/fil%20e.html}"));

        /**
         * DOI Test cases
         */
        Assert.assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("10.1109/VLHCC.2004.20"));
        Assert.assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("doi://10.1109/VLHCC.2004.20"));
        Assert.assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("doi:/10.1109/VLHCC.2004.20"));
        Assert.assertEquals("http://dx.doi.org/10.1109/VLHCC.2004.20", Util.sanitizeUrl("doi:10.1109/VLHCC.2004.20"));

        /**
         * Additional testcases provided by Hannes Restel and Micha Beckmann.
         */
        Assert.assertEquals("ftp://www.vg.no", Util.sanitizeUrl("ftp://www.vg.no"));
        Assert.assertEquals("file://doof.txt", Util.sanitizeUrl("file://doof.txt"));
        Assert.assertEquals("file:///", Util.sanitizeUrl("file:///"));
        Assert.assertEquals("/src/doof.txt", Util.sanitizeUrl("/src/doof.txt"));
        Assert.assertEquals("/", Util.sanitizeUrl("/"));
        Assert.assertEquals("/home/user/example.txt", Util.sanitizeUrl("/home/user/example.txt"));
    }

    @Test
    public void testToUpperCharFirst() {

        Assert.assertEquals("", Util.toUpperFirstLetter(""));
        Assert.assertEquals("A", Util.toUpperFirstLetter("a"));
        Assert.assertEquals("A", Util.toUpperFirstLetter("A"));
        Assert.assertEquals("An", Util.toUpperFirstLetter("an"));
        Assert.assertEquals("AN", Util.toUpperFirstLetter("AN"));
        Assert.assertEquals("TestTest", Util.toUpperFirstLetter("testTest"));

    }

}
