package net.sf.jabref.logic.bibtex;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Set;

import net.sf.jabref.Globals;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

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
        writer = new BibEntryWriter(
                new LatexFieldFormatter(LatexFieldFormatterPreferences.fromPreferences(Globals.prefs)), true);
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
        String expected = StringUtil.NEWLINE + "@Article{," + StringUtil.NEWLINE +
                "  author  = {Foo Bar}," + StringUtil.NEWLINE +
                "  journal = {International Journal of Something}," + StringUtil.NEWLINE +
                "  number  = {1}," + StringUtil.NEWLINE +
                "  note    = {some note}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

    @Test
    public void roundTripTest() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}" + StringUtil.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void roundTripWithPrependingNewlines() throws IOException {
        // @formatter:off
        String bibtexEntry = "\r\n@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}" + StringUtil.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void roundTripWithModification() throws IOException {
        // @formatter:off
        String bibtexEntry = StringUtil.NEWLINE + "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}," + StringUtil.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // Modify entry
        entry.setField("author", "BlaBla");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = StringUtil.NEWLINE + "@Article{test," + StringUtil.NEWLINE +
                "  author  = {BlaBla}," + StringUtil.NEWLINE +
                "  journal = {International Journal of Something}," + StringUtil.NEWLINE +
                "  number  = {1}," + StringUtil.NEWLINE +
                "  note    = {some note}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void roundTripWithCamelCasingInTheOriginalEntryAndResultInLowerCase() throws IOException {
        // @formatter:off
        String bibtexEntry = StringUtil.NEWLINE + "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}," + StringUtil.NEWLINE +
                "  HowPublished             = {asdf}," + StringUtil.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField("author", "BlaBla");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = StringUtil.NEWLINE + "@Article{test," + StringUtil.NEWLINE +
                "  author       = {BlaBla}," + StringUtil.NEWLINE +
                "  journal      = {International Journal of Something}," + StringUtil.NEWLINE +
                "  number       = {1}," + StringUtil.NEWLINE +
                "  note         = {some note}," + StringUtil.NEWLINE +
                "  howpublished = {asdf}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void testEntryTypeChange() throws IOException {
        // @formatter:off
        String expected = StringUtil.NEWLINE + "@Article{test," + StringUtil.NEWLINE +
                "  author       = {BlaBla}," + StringUtil.NEWLINE +
                "  journal      = {International Journal of Something}," + StringUtil.NEWLINE +
                "  number       = {1}," + StringUtil.NEWLINE +
                "  note         = {some note}," + StringUtil.NEWLINE +
                "  howpublished = {asdf}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(expected));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setType("inproceedings");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expectedNewEntry = StringUtil.NEWLINE + "@InProceedings{test," + StringUtil.NEWLINE +
                "  author       = {BlaBla}," + StringUtil.NEWLINE +
                "  number       = {1}," + StringUtil.NEWLINE +
                "  note         = {some note}," + StringUtil.NEWLINE +
                "  howpublished = {asdf}," + StringUtil.NEWLINE +
                "  journal      = {International Journal of Something}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;
        // @formatter:on
        assertEquals(expectedNewEntry, actual);
    }


    @Test
    public void roundTripWithAppendedNewlines() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}" + StringUtil.NEWLINE +
                "}\n\n";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

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
        String bibtexEntry = "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}" + StringUtil.NEWLINE +
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
        BibEntry entry = entries.iterator().next();

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
        String bibtexEntry = "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Month                    = mar," + StringUtil.NEWLINE +
                "  Number                   = {1}" + StringUtil.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify month field
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("month"));
        assertEquals("#mar#", entry.getFieldOptional("month").get());

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void addFieldWithLongerLength() throws IOException {
        // @formatter:off
        String bibtexEntry = StringUtil.NEWLINE + StringUtil.NEWLINE + "@Article{test," + StringUtil.NEWLINE +
                "  author =  {BlaBla}," + StringUtil.NEWLINE +
                "  journal = {International Journal of Something}," + StringUtil.NEWLINE +
                "  number =  {1}," + StringUtil.NEWLINE +
                "  note =    {some note}," + StringUtil.NEWLINE +
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
        String expected = StringUtil.NEWLINE + "@Article{test," + StringUtil.NEWLINE +
                "  author       = {BlaBla}," + StringUtil.NEWLINE +
                "  journal      = {International Journal of Something}," + StringUtil.NEWLINE +
                "  number       = {1}," + StringUtil.NEWLINE +
                "  note         = {some note}," + StringUtil.NEWLINE +
                "  howpublished = {asdf}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;
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

        String expected = StringUtil.NEWLINE + "@Article{," + StringUtil.NEWLINE +
                "  note   = {some note}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;

        assertEquals(expected, actual);
    }

    @Test
    public void trimFieldContents() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("1234", "article");
        entry.setField("note", "        some note    \t");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        String expected = StringUtil.NEWLINE + "@Article{," + StringUtil.NEWLINE +
                "  note = {some note}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;

        assertEquals(expected, actual);
    }

    @Test
    public void roundTripWithPrecedingCommentTest() throws IOException {
        // @formatter:off
        String bibtexEntry = "% Some random comment that should stay here" + StringUtil.NEWLINE +
                "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}" + StringUtil.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void roundTripWithPrecedingCommentAndModificationTest() throws IOException {
        // @formatter:off
        String bibtexEntry = "% Some random comment that should stay here" + StringUtil.NEWLINE +
                "@Article{test," + StringUtil.NEWLINE +
                "  Author                   = {Foo Bar}," + StringUtil.NEWLINE +
                "  Journal                  = {International Journal of Something}," + StringUtil.NEWLINE +
                "  Note                     = {some note}," + StringUtil.NEWLINE +
                "  Number                   = {1}" + StringUtil.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // change the entry
        entry.setField("author", "John Doe");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();
        // @formatter:off
        String expected = "% Some random comment that should stay here" + StringUtil.NEWLINE + StringUtil.NEWLINE +
                "@Article{test," + StringUtil.NEWLINE +
                "  author  = {John Doe}," + StringUtil.NEWLINE +
                "  journal = {International Journal of Something}," + StringUtil.NEWLINE +
                "  number  = {1}," + StringUtil.NEWLINE +
                "  note    = {some note}," + StringUtil.NEWLINE +
                "}" + StringUtil.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

}
