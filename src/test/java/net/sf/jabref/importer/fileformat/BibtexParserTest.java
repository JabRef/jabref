package net.sf.jabref.importer.fileformat;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Test the BibtexParser
 *
 * @author Christopher Oezbek <oezi@oezi.de>
 * @version $revision: 1.1$ $date: $
 */
public class BibtexParserTest {

    @Test
    public void testParseReader() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(
                "@article{test,author={Ed von Test}}"));

        Collection<BibtexEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibtexEntry e = c.iterator().next();
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Set<String> o = e.getFieldNames();
        Assert.assertTrue(o.contains("author"));
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void testBibtexParser() {
        try {
            new BibtexParser(null);
            Assert.fail("Should not accept null.");
        } catch (NullPointerException ignored) {
            // Ignored
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        Assert.assertTrue(BibtexParser
                .isRecognizedFormat(new StringReader(
                        "This file was created with JabRef 2.1 beta 2."
                                + "\n"
                                + "Encoding: Cp1252"
                                + "\n"
                                + ""
                                + "\n"
                                + "@INPROCEEDINGS{CroAnnHow05,"
                                + "\n"
                                + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                                + "\n"
                                + "  title = {Effective work practices for floss development: A model and propositions},"
                                + "\n"
                                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                                + "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
                                + "  timestamp = {2006.05.29}," + "\n"
                                + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"
                )));

        Assert.assertTrue(BibtexParser
                .isRecognizedFormat(new StringReader(
                        "@INPROCEEDINGS{CroAnnHow05,"
                                + "\n"
                                + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                                + "\n"
                                + "  title = {Effective work practices for floss development: A model and propositions},"
                                + "\n"
                                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                                + "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
                                + "  timestamp = {2006.05.29}," + "\n"
                                + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"
                )));

        Assert.assertFalse(BibtexParser
                .isRecognizedFormat(new StringReader(
                        "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                                + "\n"
                                + "  title = {Effective work practices for floss development: A model and propositions},"
                                + "\n"
                                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                                + "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
                                + "  timestamp = {2006.05.29}," + "\n"
                                + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"
                )));

        Assert.assertFalse(BibtexParser.isRecognizedFormat(new StringReader(
                "This was created with JabRef 2.1 beta 2." + "\n" + "Encoding: Cp1252" + "\n")));
    }

    @Test
    public void testFromString() throws Exception {

        { // Simple case
            Collection<BibtexEntry> c = BibtexParser.fromString("@article{test,author={Ed von Test}}");
            Assert.assertEquals(1, c.size());

            BibtexEntry e = c.iterator().next();
            Assert.assertEquals("test", e.getCiteKey());
            Assert.assertEquals(2, e.getFieldNames().size());
            Assert.assertTrue(e.getFieldNames().contains("author"));
            Assert.assertEquals("Ed von Test", e.getField("author"));
        }
        { // Empty String
            Collection<BibtexEntry> c = BibtexParser.fromString("");
            Assert.assertEquals(0, c.size());

        }
        // Error
        Collection<BibtexEntry> c = BibtexParser.fromString("@@article@@{{{{{{}");
        Assert.assertEquals(null, c);

    }

    @Test
    public void testFromSingle2() {
        /**
         * More
         */
        Collection<BibtexEntry> c = BibtexParser.fromString("@article{canh05,"
                + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n"
                + "@inProceedings{foo," + "  author={Norton Bar}}");

        Assert.assertEquals(2, c.size());

        Iterator<BibtexEntry> i = c.iterator();
        BibtexEntry a = i.next();
        BibtexEntry b = i.next();

        if (a.getCiteKey().equals("foo")) {
            BibtexEntry tmp = a;
            a = b;
            b = tmp;
        }

        Assert.assertEquals("canh05", a.getCiteKey());
        Assert.assertEquals("Crowston, K. and Annabi, H.", a.getField("author"));
        Assert.assertEquals("Title A", a.getField("title"));
        Assert.assertEquals(BibtexEntryTypes.ARTICLE, a.getType());

        Assert.assertEquals("foo", b.getCiteKey());
        Assert.assertEquals("Norton Bar", b.getField("author"));
        Assert.assertEquals(BibtexEntryTypes.INPROCEEDINGS, b.getType());
    }

    @Test
    public void testFromStringSingle() {
        BibtexEntry a = BibtexParser.singleFromString("@article{canh05,"
                + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n");

        Assert.assertEquals("canh05", a.getCiteKey());
        Assert.assertEquals("Crowston, K. and Annabi, H.", a.getField("author"));
        Assert.assertEquals("Title A", a.getField("title"));
        Assert.assertEquals(BibtexEntryTypes.ARTICLE, a.getType());

        BibtexEntry b = BibtexParser.singleFromString("@article{canh05,"
                + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n"
                + "@inProceedings{foo," + "  author={Norton Bar}}");

        if (!(b.getCiteKey().equals("canh05") || b.getCiteKey().equals("foo"))) {
            Assert.fail();
        }
    }

    @Test
    public void testParse() throws IOException {

        // Test Standard parsing
        BibtexParser parser = new BibtexParser(new StringReader(
                "@article{test,author={Ed von Test}}"));
        ParserResult result = parser.parse();

        Collection<BibtexEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibtexEntry e = c.iterator().next();
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertTrue(e.getFieldNames().contains("author"));
        Assert.assertEquals("Ed von Test", e.getField("author"));

        // Calling parse again will return the same result
        Assert.assertEquals(result, parser.parse());
    }

    @Test
    public void testParse2() throws IOException {

        BibtexParser parser = new BibtexParser(new StringReader(
                "@article{test,author={Ed von Test}}"));
        ParserResult result = parser.parse();

        BibtexEntry e = new BibtexEntry("", BibtexEntryTypes.ARTICLE);
        e.setField("author", "Ed von Test");
        e.setField("bibtexkey", "test");

        Collection<BibtexEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibtexEntry e2 = c.iterator().next();

        Assert.assertNotSame(e.getId(), e2.getId());

        for (String field : e.getFieldNames()) {
            if (!e.getField(field).equals(e2.getField(field))) {
                Assert.fail("e and e2 differ in field " + field);
            }
        }
    }

    /**
     * Test for [ 1594123 ] Failure to import big numbers
     * <p/>
     * Issue Reported by Ulf Martin.
     *
     * @throws IOException
     */
    @Test
    public void testBigNumbers() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,"
                + "isbn = 1234567890123456789,\n" + "isbn2 = {1234567890123456789},\n"
                + "small = 1234,\n" + "}"));

        Collection<BibtexEntry> c = result.getDatabase().getEntries();
        BibtexEntry e = c.iterator().next();

        Assert.assertEquals("1234567890123456789", e.getField("isbn"));
        Assert.assertEquals("1234567890123456789", e.getField("isbn2"));
        Assert.assertEquals("1234", e.getField("small"));
    }

    @Test
    public void testBigNumbers2() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(""
                + "@string{bourdieu = {Bourdieu, Pierre}}"
                + "@book{bourdieu-2002-questions-sociologie, " + "	Address = {Paris},"
                + "	Author = bourdieu," + "	Isbn = 2707318256," + "	Publisher = {Minuit},"
                + "	Title = {Questions de sociologie}," + "	Year = 2002" + "}"));

        Collection<BibtexEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibtexEntry e = c.iterator().next();

        Assert.assertEquals("bourdieu-2002-questions-sociologie", e.getCiteKey());
        Assert.assertEquals(BibtexEntryTypes.BOOK, e.getType());
        Assert.assertEquals("2707318256", e.getField("isbn"));
        Assert.assertEquals("Paris", e.getField("address"));
        Assert.assertEquals("Minuit", e.getField("publisher"));
        Assert.assertEquals("Questions de sociologie", e.getField("title"));
        Assert.assertEquals("#bourdieu#", e.getField("author"));
        Assert.assertEquals("2002", e.getField("year"));
    }

    @Test
    @Ignore
    public void testNewlineHandling() {

        BibtexEntry e = BibtexParser.singleFromString("@article{canh05," +
                "a = {a\nb}," +
                "b = {a\n\nb}," +
                "c = {a\n \nb}," +
                "d = {a \n \n b},"
                + "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
                + "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
                + "file = {Bemerkung:H:\\bla\\ups  sala.pdf:PDF}, \n"
                + "}");

        Assert.assertEquals("canh05", e.getCiteKey());
        Assert.assertEquals(BibtexEntryTypes.ARTICLE, e.getType());

        Assert.assertEquals("a b", e.getField("a"));
        Assert.assertEquals("a\nb", e.getField("b"));
        Assert.assertEquals("a b", e.getField("c"));
        Assert.assertEquals("a b", e.getField("d"));

        // I think the last \n is a bug in the parser...
        Assert.assertEquals("Hallo World this is\nnot \nan \n exercise . \n\n", e.getField("title"));
        Assert.assertEquals("Hallo World this isnot an exercise . ", e.getField("tabs"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    public void testFileNaming() {
        BibtexEntry e = BibtexParser.singleFromString("@article{canh05,"
                + "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
                + "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
                + "file = {Bemerkung:H:\\bla\\ups  sala.pdf:PDF}, \n"
                + "}");

        Assert.assertEquals("Bemerkung:H:\\bla\\ups  sala.pdf:PDF", e.getField("file"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    public void testFileNaming1() {
        BibtexEntry e = BibtexParser.singleFromString("@article{canh05,"
                + "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
                + "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
                + "file = {Bemerkung:H:\\bla\\ups  \tsala.pdf:PDF}, \n"
                + "}");

        Assert.assertEquals("Bemerkung:H:\\bla\\ups  \tsala.pdf:PDF", e.getField("file"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    @Ignore
    public void testFileNaming3() {
        BibtexEntry e = BibtexParser.singleFromString("@article{canh05,"
                + "title = {\nHallo \nWorld \nthis \n is\n\nnot \n\nan \n\n exercise \n \n.\n \n\n},\n"
                + "tabs = {\nHallo \tWorld \tthis \t is\t\tnot \t\tan \t\n exercise \t \n.\t \n\t},\n"
                + "file = {Bemerkung:H:\\bla\\ups \n\tsala.pdf:PDF}, \n"
                + "}");

        Assert.assertEquals("Bemerkung:H:\\bla\\ups  sala.pdf:PDF", e.getField("file"));
    }
}