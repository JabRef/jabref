package org.jabref.logic.bibtex;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Set;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class BibEntryWriterTest {

    private static ImportFormatPreferences importFormatPreferences;
    private BibEntryWriter writer;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    @BeforeEach
    public void setUpWriter() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        LatexFieldFormatterPreferences latexFieldFormatterPreferences = mock(LatexFieldFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        writer = new BibEntryWriter(new LatexFieldFormatter(latexFieldFormatterPreferences), true);
    }

    @Test
    public void testSerialization() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("article");
        //set a required field
        entry.setField("author", "Foo Bar");
        entry.setField("journal", "International Journal of Something");
        //set an optional field
        entry.setField("number", "1");
        entry.setField("note", "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{," + OS.NEWLINE +
                "  author  = {Foo Bar}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

    @Test
    public void writeOtherTypeTest() throws Exception {
        String expected = OS.NEWLINE + "@Other{test," + OS.NEWLINE +
                "  comment = {testentry}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        BibEntry entry = new BibEntry();
        entry.setType("other");
        entry.setField("Comment", "testentry");
        entry.setCiteKey("test");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void writeReallyunknownTypeTest() throws Exception {
        String expected = OS.NEWLINE + "@Reallyunknowntype{test," + OS.NEWLINE +
                "  comment = {testentry}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        BibEntry entry = new BibEntry();
        entry.setType("ReallyUnknownType");
        entry.setField("Comment", "testentry");
        entry.setCiteKey("test");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void roundTripTest() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
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
        String bibtexEntry = "\r\n@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
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
        String bibtexEntry = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // Modify entry
        entry.setField("author", "BlaBla");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author  = {BlaBla}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void roundTripWithCamelCasingInTheOriginalEntryAndResultInLowerCase() throws IOException {
        // @formatter:off
        String bibtexEntry = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  HowPublished             = {asdf}," + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField("author", "BlaBla");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void testEntryTypeChange() throws IOException {
        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(expected));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setType("inproceedings");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expectedNewEntry = OS.NEWLINE + "@InProceedings{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expectedNewEntry, actual);
    }

    @Test
    public void roundTripWithAppendedNewlines() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}\n\n";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
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
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        String result = testSingleWrite(bibtexEntry);
        result = testSingleWrite(result);
        result = testSingleWrite(result);

        assertEquals(bibtexEntry, result);
    }

    private String testSingleWrite(String bibtexEntry) throws IOException {
        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
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
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Month                    = mar," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify month field
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("month"));
        assertEquals("#mar#", entry.getField("month").get());

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    public void addFieldWithLongerLength() throws IOException {
        // @formatter:off
        String bibtexEntry = OS.NEWLINE + OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author =  {BlaBla}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  number =  {1}," + OS.NEWLINE +
                "  note =    {some note}," + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField("howpublished", "asdf");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    public void doNotWriteEmptyFields() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("article");
        entry.setField("author", "  ");
        entry.setField("note", "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        String expected = OS.NEWLINE + "@Article{," + OS.NEWLINE +
                "  note   = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        assertEquals(expected, actual);
    }

    @Test
    public void trimFieldContents() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("article");
        entry.setField("note", "        some note    \t");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        String expected = OS.NEWLINE + "@Article{," + OS.NEWLINE +
                "  note = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        assertEquals(expected, actual);
    }

    @Test
    public void writeThrowsErrorIfFieldContainsUnbalancedBraces() {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("article");
        entry.setField("note", "some text with unbalanced { braces");

        assertThrows(IOException.class, () -> writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void roundTripWithPrecedingCommentTest() throws IOException {
        // @formatter:off
        String bibtexEntry = "% Some random comment that should stay here" + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
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
        String bibtexEntry = "% Some random comment that should stay here" + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // change the entry
        entry.setField("author", "John Doe");

        //write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();
        // @formatter:off
        String expected = "% Some random comment that should stay here" + OS.NEWLINE + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  author  = {John Doe}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

}
