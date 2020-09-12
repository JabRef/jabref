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
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BibEntryWriterTest {

    private static ImportFormatPreferences importFormatPreferences;
    private BibEntryWriter writer;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    @BeforeEach
    void setUpWriter() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FieldWriterPreferences fieldWriterPreferences = mock(FieldWriterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        writer = new BibEntryWriter(new FieldWriter(fieldWriterPreferences), new BibEntryTypesManager());
    }

    @Test
    void testSerialization() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        // set a required field
        entry.setField(StandardField.AUTHOR, "Foo Bar");
        entry.setField(StandardField.JOURNAL, "International Journal of Something");
        // set an optional field
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.NOTE, "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{," + OS.NEWLINE +
                "  author  = {Foo Bar}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

    @Test
    void writeOtherTypeTest() throws Exception {
        String expected = OS.NEWLINE + "@Other{test," + OS.NEWLINE +
                "  comment = {testentry}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        BibEntry entry = new BibEntry(new UnknownEntryType("other"));
        entry.setField(StandardField.COMMENT, "testentry");
        entry.setCitationKey("test");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(expected, actual);
    }

    @Test
    void writeEntryWithFile() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        LinkedFile file = new LinkedFile("test", "/home/uers/test.pdf", "PDF");
        entry.addFile(file);

        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        assertEquals(OS.NEWLINE +
                "@Article{,"
                + OS.NEWLINE
                + "  file = {test:/home/uers/test.pdf:PDF},"
                + OS.NEWLINE
                + "}" + OS.NEWLINE, stringWriter.toString());
    }

    @Test
    void writeEntryWithOrField() throws Exception {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry(StandardEntryType.InBook);
        // set an required OR field (author/editor)
        entry.setField(StandardField.EDITOR, "Foo Bar");
        entry.setField(StandardField.JOURNAL, "International Journal of Something");
        // set an optional field
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.NOTE, "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@InBook{," + OS.NEWLINE +
                "  editor  = {Foo Bar}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

    @Test
    void writeEntryWithOrFieldBothFieldsPresent() throws Exception {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry(StandardEntryType.InBook);
        // set an required OR field with both fields(author/editor)
        entry.setField(StandardField.AUTHOR, "Foo Thor");
        entry.setField(StandardField.EDITOR, "Edi Bar");
        entry.setField(StandardField.JOURNAL, "International Journal of Something");
        // set an optional field
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.NOTE, "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@InBook{," + OS.NEWLINE +
                "  author  = {Foo Thor}," + OS.NEWLINE +
                "  editor  = {Edi Bar}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

    @Test
    void writeReallyUnknownTypeTest() throws Exception {
        String expected = OS.NEWLINE + "@Reallyunknowntype{test," + OS.NEWLINE +
                "  comment = {testentry}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        BibEntry entry = new BibEntry();
        entry.setType(new UnknownEntryType("ReallyUnknownType"));
        entry.setField(StandardField.COMMENT, "testentry");
        entry.setCitationKey("test");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(expected, actual);
    }

    @Test
    void roundTripTest() throws IOException {
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

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    void roundTripWithPrependingNewlines() throws IOException {
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

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    void roundTripWithModification() throws IOException {
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
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author  = {BlaBla}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    void roundTripWithCamelCasingInTheOriginalEntryAndResultInLowerCase() throws IOException {
        // @formatter:off
        String bibtexEntry = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  HowPublished             = {asdf}," + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    void testEntryTypeChange() throws IOException {
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
        entry.setType(StandardEntryType.InProceedings);

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expectedNewEntry = OS.NEWLINE + "@InProceedings{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expectedNewEntry, actual);
    }

    @Test
    void roundTripWithAppendedNewlines() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  Note                     = {some note}" + OS.NEWLINE +
                "}\n\n";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // Only one appending newline is written by the writer, the rest by FileActions. So, these should be removed here.
        assertEquals(bibtexEntry.substring(0, bibtexEntry.length() - 1), actual);
    }

    @Test
    void multipleWritesWithoutModification() throws IOException {
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

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
        return actual;
    }

    @Test
    void monthFieldSpecialSyntax() throws IOException {
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
        Set<Field> fields = entry.getFields();
        assertTrue(fields.contains(StandardField.MONTH));
        assertEquals("#mar#", entry.getField(StandardField.MONTH).get());

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    void constantMonthApril() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.MONTH, "#apr#");
        // enable writing
        entry.setChanged(true);

        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        assertEquals(OS.NEWLINE +
                        "@Misc{," + OS.NEWLINE +
                        "  month = apr," + OS.NEWLINE +
                        "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void monthApril() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.MONTH, "apr");
        // enable writing
        entry.setChanged(true);

        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        assertEquals(OS.NEWLINE +
                        "@Misc{," + OS.NEWLINE +
                        "  month = {apr}," + OS.NEWLINE +
                        "}" + OS.NEWLINE,
                stringWriter.toString());
    }

    @Test
    void addFieldWithLongerLength() throws IOException {
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
        entry.setField(StandardField.HOWPUBLISHED, "asdf");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expected, actual);
    }

    @Test
    void doNotWriteEmptyFields() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "  ");
        entry.setField(StandardField.NOTE, "some note");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);

        String actual = stringWriter.toString();

        String expected = OS.NEWLINE + "@Article{," + OS.NEWLINE +
                "  note   = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        assertEquals(expected, actual);
    }

    @Test
    void writeThrowsErrorIfFieldContainsUnbalancedBraces() {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.NOTE, "some text with unbalanced { braces");

        assertThrows(IOException.class, () -> writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX));
    }

    @Test
    void roundTripWithPrecedingCommentTest() throws IOException {
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

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        assertEquals(bibtexEntry, actual);
    }

    @Test
    void roundTripWithPrecedingCommentAndModificationTest() throws IOException {
        // @formatter:off
        String bibtexEntry = "% Some random comment that should stay here" + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  Note                     = {some note}" + OS.NEWLINE +
                "}";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences, fileMonitor).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // change the entry
        entry.setField(StandardField.AUTHOR, "John Doe");

        // write out bibtex string
        StringWriter stringWriter = new StringWriter();
        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();
        // @formatter:off
        String expected = "% Some random comment that should stay here" + OS.NEWLINE + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  author  = {John Doe}," + OS.NEWLINE +
                "  journal = {International Journal of Something}," + OS.NEWLINE +
                "  note    = {some note}," + OS.NEWLINE +
                "  number  = {1}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }

    @Test
    void alphabeticSerialization() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        // required fields
        entry.setField(StandardField.AUTHOR, "Foo Bar");
        entry.setField(StandardField.JOURNALTITLE, "International Journal of Something");
        entry.setField(StandardField.TITLE, "Title");
        entry.setField(StandardField.DATE, "2019-10-16");
        // optional fields
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.NOTE, "some note");
        // unknown fields
        entry.setField(StandardField.YEAR, "2019");
        entry.setField(StandardField.CHAPTER, "chapter");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBLATEX);

        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{," + OS.NEWLINE +
                "  author       = {Foo Bar}," + OS.NEWLINE +
                "  date         = {2019-10-16}," + OS.NEWLINE +
                "  journaltitle = {International Journal of Something}," + OS.NEWLINE +
                "  title        = {Title}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  chapter      = {chapter}," + OS.NEWLINE +
                "  year         = {2019}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }
}
