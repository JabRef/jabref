package net.sf.jabref.bibtex;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class BibEntryWriterTest {

    private BibtexEntryWriter writer;
    private static JabRefPreferences backup;

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        backup = Globals.prefs;
        // make sure that we use the "new style" serialization
        Globals.prefs.putInt(JabRefPreferences.WRITEFIELD_SORTSTYLE, 0);
        // make sure that we use camel casing
        Globals.prefs.putBoolean(JabRefPreferences.WRITEFIELD_CAMELCASENAME, true);
    }

    @AfterClass
    public static void tearDown() {
        Globals.prefs.overwritePreferences(backup);
    }

    @Before
    public void setUpWriter() {
        writer = new BibtexEntryWriter(new LatexFieldFormatter(), true);
    }

    @Test
    public void testSerialization() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("1234", EntryTypes.getType("Article"));
        //set a required field
        entry.setField("author", "Foo Bar");
        entry.setField("journal", "International Journal of Something");
        //set an optional field
        entry.setField("number", "1");
        entry.setField("note", "some note");

        writer.write(entry, stringWriter);

        String actual = stringWriter.toString();

        String expected = Globals.NEWLINE + Globals.NEWLINE + "@Article{," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";

        assertEquals(expected, actual);
    }

    @Test
    public void roundTripTest() throws IOException {
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("test", entry.getCiteKey());
        Assert.assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        Assert.assertTrue(fields.contains("author"));
        Assert.assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void roundTripWithPrependingNewlines() throws IOException {
        String bibtexEntry = "\r\n@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("test", entry.getCiteKey());
        Assert.assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        Assert.assertTrue(fields.contains("author"));
        Assert.assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void roundTripWithModification() throws IOException {
        String bibtexEntry = Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("test", entry.getCiteKey());
        Assert.assertEquals(5, entry.getFieldNames().size());
        entry.setField("author", "BlaBla");
        Set<String> fields = entry.getFieldNames();
        Assert.assertTrue(fields.contains("author"));
        Assert.assertEquals("BlaBla", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter);
        String actual = stringWriter.toString();

        String expected = Globals.NEWLINE + Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  Author                   = {BlaBla}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    public void roundTripWithCamelCasing() throws IOException {
        String bibtexEntry = Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  author                   = {Foo Bar}," + Globals.NEWLINE +
                "  journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}," + Globals.NEWLINE +
                "  howpublished             = {asdf}" + Globals.NEWLINE +
                "}";

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("test", entry.getCiteKey());
        Assert.assertEquals(6, entry.getFieldNames().size());
        entry.setField("author", "BlaBla");
        Set<String> fields = entry.getFieldNames();
        Assert.assertTrue(fields.contains("author"));
        Assert.assertEquals("BlaBla", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();

        writer.write(entry, stringWriter);
        String actual = stringWriter.toString();

        String expected = Globals.NEWLINE + Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  Author                   = {BlaBla}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}," + Globals.NEWLINE +
                "  HowPublished             = {asdf}" + Globals.NEWLINE +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    public void roundTripWithAppendedNewlines() throws IOException {
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}\n\n";

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("test", entry.getCiteKey());
        Assert.assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        Assert.assertTrue(fields.contains("author"));
        Assert.assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter);
        String actual = stringWriter.toString();

        //appending newlines are not written by the writer, but by FileActions. So, these should be removed here.
        assertEquals(bibtexEntry.substring(0, bibtexEntry.length() - 2), actual);
    }

    @Test
    public void multipleWritesWithoutModification() throws IOException {
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";

        String result = testSingleWrite(bibtexEntry);
        result = testSingleWrite(result);
        result = testSingleWrite(result);

        assertEquals(bibtexEntry, result);
    }

    private String testSingleWrite(String bibtexEntry) throws IOException {
        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("test", entry.getCiteKey());
        Assert.assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        Assert.assertTrue(fields.contains("author"));
        Assert.assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
        return actual;
    }

    @Test
    public void monthFieldSpecialSyntax() throws IOException {
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Month                    = mar," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        Assert.assertEquals("test", entry.getCiteKey());
        Assert.assertEquals(4, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        Assert.assertTrue(fields.contains("month"));
        Assert.assertEquals("#mar#", entry.getField("month"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }
}
