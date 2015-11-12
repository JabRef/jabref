package net.sf.jabref.bibtex;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibtexEntry;
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

public class BibtexEntryWriterTest {

    private BibtexEntryWriter writer;

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        // make sure that we use the "new style" serialization
        Globals.prefs.putInt(JabRefPreferences.WRITEFIELD_SORTSTYLE, 0);

    }

    @Before
    public void setUpWriter() {
        writer = new BibtexEntryWriter(new LatexFieldFormatter(), true);
    }

    @Test
    public void testSerialization() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibtexEntry entry = new BibtexEntry("1234", EntryTypes.getType("Article"));
        //set a required field
        entry.setField("author", "Foo Bar");
        entry.setField("journal", "International Journal of Something");
        //set an optional field
        entry.setField("number", "1");
        entry.setField("note", "some note");

        writer.write(entry, stringWriter);

        String actual = stringWriter.toString();

        String expected = "@Article{," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}" + Globals.NEWLINE;

        assertEquals(expected, actual);
    }

    @Test
    public void roundTripTest() throws IOException {
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}" + Globals.NEWLINE;

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibtexEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibtexEntry entry = entries.iterator().next();
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
    public void monthFieldSpecialSyntax() throws IOException {
        String bibtexEntry = "@Article{test," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Month                    = mar," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}" + Globals.NEWLINE;

        // read in bibtex string
        ParserResult result = BibtexParser.parse(new StringReader(bibtexEntry));

        Collection<BibtexEntry> entries = result.getDatabase().getEntries();
        Assert.assertEquals(1, entries.size());

        BibtexEntry entry = entries.iterator().next();
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
