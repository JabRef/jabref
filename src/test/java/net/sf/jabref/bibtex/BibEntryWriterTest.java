package net.sf.jabref.bibtex;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Set;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BibEntryWriterTest {

    private BibEntryWriter writer;
    private static JabRefPreferences backup;

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        backup = Globals.prefs;
    }

    @AfterClass
    public static void tearDown() {
        Globals.prefs.overwritePreferences(backup);
    }

    @Before
    public void setUpWriter() {
        writer = new BibEntryWriter(new LatexFieldFormatter(), true);
    }

    @Test
    public void testSerialization() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("1234", "article");
        //set a required field
        entry.setField("author", "Foo Bar");
        entry.setField("journal", "International Journal of Something");
        //set an optional field
        entry.setField("number", "1");
        entry.setField("note", "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        // @formatter:off
        String expected = Globals.NEWLINE + "@Article{," + Globals.NEWLINE +
                "  author  = {Foo Bar}," + Globals.NEWLINE +
                "  journal = {International Journal of Something}," + Globals.NEWLINE +
                "  number  = {1}," + Globals.NEWLINE +
                "  note    = {some note}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

    @Test
    public void roundTripTest() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals("test", entry.getCiteKey());
        assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void roundTripWithPrependingNewlines() throws IOException {
        // @formatter:off
        String bibtexEntry = "\r\n@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals("test", entry.getCiteKey());
        assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void roundTripWithModification() throws IOException {
        // @formatter:off
        String bibtexEntry = Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}," + Globals.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals("test", entry.getCiteKey());
        assertEquals(5, entry.getFieldNames().size());

        // Modify entry
        entry.setField("author", "BlaBla");

        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals("BlaBla", entry.getField("author"));

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  author  = {BlaBla}," + Globals.NEWLINE +
                "  journal = {International Journal of Something}," + Globals.NEWLINE +
                "  number  = {1}," + Globals.NEWLINE +
                "  note    = {some note}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void roundTripWithCamelCasingInTheOriginalEntryAndResultInLowerCase() throws IOException {
        // @formatter:off
        String bibtexEntry = Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}," + Globals.NEWLINE +
                "  HowPublished             = {asdf}," + Globals.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals("test", entry.getCiteKey());
        assertEquals(6, entry.getFieldNames().size());

        // modify entry
        entry.setField("author", "BlaBla");

        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals("BlaBla", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  author       = {BlaBla}," + Globals.NEWLINE +
                "  journal      = {International Journal of Something}," + Globals.NEWLINE +
                "  number       = {1}," + Globals.NEWLINE +
                "  note         = {some note}," + Globals.NEWLINE +
                "  howpublished = {asdf}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void roundTripWithAppendedNewlines() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}\n\n";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals("test", entry.getCiteKey());
        assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // Only one appending newline is written by the writer, the rest by FileActions. So, these should be removed here.
        assertEquals(bibtexEntry.substring(0, bibtexEntry.length() - 1), actual);
    }

    @Test
    public void multipleWritesWithoutModification() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";
        // @formatter:on

        String result = testSingleWrite(bibtexEntry);
        result = testSingleWrite(result);
        result = testSingleWrite(result);

        assertEquals(bibtexEntry, result);
    }

    private String testSingleWrite(String bibtexEntry) throws IOException {
        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals("test", entry.getCiteKey());
        assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals("Foo Bar", entry.getField("author"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
        return actual;
    }

    @Test
    public void monthFieldSpecialSyntax() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Month                    = mar," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals("test", entry.getCiteKey());
        assertEquals(4, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("month"));
        assertEquals("#mar#", entry.getField("month"));

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void addFieldWithLongerLength() throws IOException {
        // @formatter:off
        String bibtexEntry = Globals.NEWLINE + Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  author =  {BlaBla}," + Globals.NEWLINE +
                "  journal = {International Journal of Something}," + Globals.NEWLINE +
                "  number =  {1}," + Globals.NEWLINE +
                "  note =    {some note}," + Globals.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField("howpublished", "asdf");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = Globals.NEWLINE + "@Article{test," + Globals.NEWLINE +
                "  author       = {BlaBla}," + Globals.NEWLINE +
                "  journal      = {International Journal of Something}," + Globals.NEWLINE +
                "  number       = {1}," + Globals.NEWLINE +
                "  note         = {some note}," + Globals.NEWLINE +
                "  howpublished = {asdf}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void doNotWriteEmptyFields() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("1234", "article");
        entry.setField("author", "  ");
        entry.setField("note", "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        String expected = Globals.NEWLINE + "@Article{," + Globals.NEWLINE +
                "  note   = {some note}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE;

        assertEquals(expected, actual);
    }

    @Test
    public void trimFieldContents() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("1234", "article");
        entry.setField("note", "        some note    \t");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        String expected = Globals.NEWLINE + "@Article{," + Globals.NEWLINE +
                "  note = {some note}," + Globals.NEWLINE +
                "}" + Globals.NEWLINE;

        assertEquals(expected, actual);
    }
}
