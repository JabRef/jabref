package org.jabref.logic.bibtex;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BibEntryWriterTest {

    private static ImportFormatPreferences importFormatPreferences;
    private final StringWriter stringWriter = new StringWriter();
    private BibWriter bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
    private BibEntryWriter bibEntryWriter;

    @BeforeEach
    void setUpWriter() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FieldPreferences fieldPreferences = new FieldPreferences(true, List.of(StandardField.MONTH), Collections.emptyList());
        bibEntryWriter = new BibEntryWriter(new FieldWriter(fieldPreferences), new BibEntryTypesManager());
    }

    @Test
    void testSerialization() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        // set a required field
        entry.setField(StandardField.AUTHOR, "Foo Bar");
        entry.setField(StandardField.JOURNAL, "International Journal of Something");
        // set an optional field
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.NOTE, "some note");

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expected = """
                @Article{,
                  author  = {Foo Bar},
                  journal = {International Journal of Something},
                  note    = {some note},
                  number  = {1},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeOtherTypeTest() throws Exception {
        String expected = """
                @Other{test,
                  comment = {testentry},
                }
                """.replaceAll("\n", OS.NEWLINE);

        BibEntry entry = new BibEntry(new UnknownEntryType("other"));
        entry.setField(StandardField.COMMENT, "testentry");
        entry.setCitationKey("test");

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeEntryWithFile() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        LinkedFile file = new LinkedFile("test", Path.of("/home/uers/test.pdf"), "PDF");
        entry.addFile(file);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals("""
                @Article{,
                  file = {test:/home/uers/test.pdf:PDF},
                }
                """.replaceAll("\n", OS.NEWLINE), stringWriter.toString());
    }

    @Test
    void writeEntryWithOrField() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InBook);
        // set an required OR field (author/editor)
        entry.setField(StandardField.EDITOR, "Foo Bar");
        entry.setField(StandardField.JOURNAL, "International Journal of Something");
        // set an optional field
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.NOTE, "some note");

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expected = """
                @InBook{,
                  editor  = {Foo Bar},
                  note    = {some note},
                  number  = {1},
                  journal = {International Journal of Something},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeEntryWithOrFieldBothFieldsPresent() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InBook);
        // set an required OR field with both fields(author/editor)
        entry.setField(StandardField.AUTHOR, "Foo Thor");
        entry.setField(StandardField.EDITOR, "Edi Bar");
        entry.setField(StandardField.JOURNAL, "International Journal of Something");
        // set an optional field
        entry.setField(StandardField.NUMBER, "1");
        entry.setField(StandardField.NOTE, "some note");

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expected = """
                @InBook{,
                  author  = {Foo Thor},
                  editor  = {Edi Bar},
                  note    = {some note},
                  number  = {1},
                  journal = {International Journal of Something},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeReallyUnknownTypeTest() throws Exception {
        String expected = """
                @Reallyunknowntype{test,
                  comment = {testentry},
                }
                """.replaceAll("\n", OS.NEWLINE);

        BibEntry entry = new BibEntry();
        entry.setType(new UnknownEntryType("ReallyUnknownType"));
        entry.setField(StandardField.COMMENT, "testentry");
        entry.setCitationKey("test");
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void roundTripTest() throws IOException {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """.replaceAll("\n", OS.NEWLINE);

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripKeepsFilePathWithBackslashes() throws IOException {
        String bibtexEntry = """
                @Article{,
                  file = {Tagungen\\2013\\KWTK45},
                }
                """.replaceAll("\n", OS.NEWLINE);

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripKeepsEscapedCharacters() throws IOException {
        String bibtexEntry = """
                @Article{,
                  demofield = {Tagungen\\2013\\KWTK45},
                }
                """.replaceAll("\n", OS.NEWLINE);

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripKeepsFilePathEndingWithBackslash() throws IOException {
        String bibtexEntry = """
                @Article{,
                  file = {dir\\},
                }
                """.replaceAll("\n", OS.NEWLINE);

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithPrependingNewlines() throws IOException {
        // @formatter:off
        String bibtexEntry = "\r\n@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry.substring(2), stringWriter.toString());
    }

    @Test
    void roundTripWithKeepsCRLFLineBreakStyle() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test,\r\n" +
                "  Author                   = {Foo Bar},\r\n" +
                "  Journal                  = {International Journal of Something},\r\n" +
                "  Note                     = {some note},\r\n" +
                "  Number                   = {1}\r\n" +
                "}\r\n";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        // need to reconfigure writer to use "\r\n"
        bibWriter = new BibWriter(stringWriter, "\r\n");
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithKeepsLFLineBreakStyle() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test,\n" +
                "  Author                   = {Foo Bar},\n" +
                "  Journal                  = {International Journal of Something},\n" +
                "  Note                     = {some note},\n" +
                "  Number                   = {1}\n" +
                "}\n";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        // need to reconfigure writer to use "\n"
        bibWriter = new BibWriter(stringWriter, "\n");
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithModification() throws IOException {
        // @formatter:off
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // Modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expected = """
                @Article{test,
                  author  = {BlaBla},
                  journal = {International Journal of Something},
                  note    = {some note},
                  number  = {1},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void roundTripWithCamelCasingInTheOriginalEntryAndResultInLowerCase() throws IOException {
        // @formatter:off
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Number                   = {1},
                  Note                     = {some note},
                  HowPublished             = {asdf},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expected = """
                @Article{test,
                  author       = {BlaBla},
                  journal      = {International Journal of Something},
                  note         = {some note},
                  number       = {1},
                  howpublished = {asdf},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void testEntryTypeChange() throws IOException {
        // @formatter:off
        String expected = """

                @Article{test,
                  author       = {BlaBla},
                  journal      = {International Journal of Something},
                  number       = {1},
                  note         = {some note},
                  howpublished = {asdf},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(expected));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setType(StandardEntryType.InProceedings);

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expectedNewEntry = """
                @InProceedings{test,
                  author       = {BlaBla},
                  note         = {some note},
                  number       = {1},
                  howpublished = {asdf},
                  journal      = {International Journal of Something},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on
        assertEquals(expectedNewEntry, stringWriter.toString());
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
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // Only one appending newline is written by the writer
        // OS.NEWLINE is used, not the given one
        assertEquals(bibtexEntry.substring(0, bibtexEntry.length() - 2) + OS.NEWLINE, actual);
    }

    @Test
    void roundTripNormalizesNewLines() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Article{test,\n" +
                "  Author                   = {Foo Bar},\r\n" +
                "  Journal                  = {International Journal of Something},\n" +
                "  Number                   = {1},\n" +
                "  Note                     = {some note}\r\n" +
                "}\n\n";
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        String expected = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  Note                     = {some note}" + OS.NEWLINE +
                "}" + OS.NEWLINE;
        assertEquals(expected, actual);
    }

    @Test
    void multipleWritesWithoutModification() throws IOException {
        // @formatter:off
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        String result = testSingleWrite(bibtexEntry);
        result = testSingleWrite(result);
        result = testSingleWrite(result);

        assertEquals(bibtexEntry, result);
    }

    private String testSingleWrite(String bibtexEntry) throws IOException {
        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String actual = writer.toString();
        assertEquals(bibtexEntry, actual);
        return actual;
    }

    @Test
    void monthFieldSpecialSyntax() throws IOException {
        // @formatter:off
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Month                    = mar,
                  Number                   = {1}
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // check month field
        Set<Field> fields = entry.getFields();
        assertTrue(fields.contains(StandardField.MONTH));
        assertEquals("#mar#", entry.getField(StandardField.MONTH).get());

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void customTypeCanBewritten() throws IOException {
        // @formatter:off
        String bibtexEntry = """
                @reference{Broecker1984,
                  title = {International Center of Photography},
                  subtitle = {Encyclopedia of Photography},
                  editor = {Broecker, William L.},
                  date = {1984},
                  eprint = {305515791},
                  eprinttype = {scribd},
                  isbn = {0-517-55271-X},
                  keywords = {g:photography, p:positive, c:silver, m:albumen, c:pigment, m:carbon, g:reference, c:encyclopedia},
                  location = {New York},
                  pagetotal = {678},
                  publisher = {Crown},
                }
                """;
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        entry.setField(FieldFactory.parseField("location"), "NY");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Reference{Broecker1984,
                  date       = {1984},
                  editor     = {Broecker, William L.},
                  eprint     = {305515791},
                  eprinttype = {scribd},
                  isbn       = {0-517-55271-X},
                  keywords   = {g:photography, p:positive, c:silver, m:albumen, c:pigment, m:carbon, g:reference, c:encyclopedia},
                  location   = {NY},
                  pagetotal  = {678},
                  publisher  = {Crown},
                  subtitle   = {Encyclopedia of Photography},
                  title      = {International Center of Photography},
                }
                """.replaceAll("\n", OS.NEWLINE);

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void constantMonthApril() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.MONTH, "#apr#");
        // enable writing
        entry.setChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals("""
                        @Misc{,
                          month = apr,
                        }
                        """.replaceAll("\n", OS.NEWLINE),
                stringWriter.toString());
    }

    @Test
    void monthApril() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.MONTH, "apr");
        // enable writing
        entry.setChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals("""
                        @Misc{,
                          month = {apr},
                        }
                        """.replaceAll("\n", OS.NEWLINE),
                stringWriter.toString());
    }

    @Test
    void filenameIsUnmodifiedDuringWrite() throws Exception {
        // source: https://github.com/JabRef/jabref/issues/7012#issuecomment-707788107
        String bibtexEntry = """
                    @Book{Hue17,
                      author    = {Rudolf Huebener},
                      date      = {2017},
                      title     = {Leiter, Halbleiter, Supraleiter},
                      doi       = {10.1007/978-3-662-53281-2},
                      publisher = {Springer Berlin Heidelberg},
                      file      = {:Hue17 - Leiter # Halbleiter # Supraleiter.pdf:PDF},
                      timestamp = {2020.10.13},
                }
                """.replaceAll("\n", OS.NEWLINE);

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
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
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // modify entry
        entry.setField(StandardField.HOWPUBLISHED, "asdf");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{test," + OS.NEWLINE +
                "  author       = {BlaBla}," + OS.NEWLINE +
                "  journal      = {International Journal of Something}," + OS.NEWLINE +
                "  note         = {some note}," + OS.NEWLINE +
                "  number       = {1}," + OS.NEWLINE +
                "  howpublished = {asdf}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void doNotWriteEmptyFields() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "  ");
        entry.setField(StandardField.NOTE, "some note");

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = "@Article{," + OS.NEWLINE +
                "  note   = {some note}," + OS.NEWLINE +
                "}" + OS.NEWLINE;

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeThrowsErrorIfFieldContainsUnbalancedBraces() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.NOTE, "some text with unbalanced { braces");

        assertThrows(IOException.class, () -> bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX));
    }

    @Test
    void roundTripWithPrecedingCommentTest() throws IOException {
        // @formatter:off
        String bibtexEntry = """
                % Some random comment that should stay here
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithPrecedingCommentAndModificationTest() throws IOException {
        // @formatter:off
        String bibtexEntry = """
                % Some random comment that should stay here
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Number                   = {1},
                  Note                     = {some note}
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        // read in bibtex string
        ParserResult result = new BibtexParser(importFormatPreferences).parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        // change the entry
        entry.setField(StandardField.AUTHOR, "John Doe");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        // @formatter:off
        String expected = """
                % Some random comment that should stay here
                @Article{test,
                  author  = {John Doe},
                  journal = {International Journal of Something},
                  note    = {some note},
                  number  = {1},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void alphabeticSerialization() throws IOException {
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

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBLATEX);

        // @formatter:off
        String expected = """
                @Article{,
                  author       = {Foo Bar},
                  date         = {2019-10-16},
                  journaltitle = {International Journal of Something},
                  title        = {Title},
                  note         = {some note},
                  number       = {1},
                  chapter      = {chapter},
                  year         = {2019},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void testSerializeAll() throws IOException {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article);
        // required fields
        entry1.setField(StandardField.AUTHOR, "Journal Author");
        entry1.setField(StandardField.JOURNALTITLE, "Journal of Words");
        entry1.setField(StandardField.TITLE, "Entry Title");
        entry1.setField(StandardField.DATE, "2020-11-16");

        // optional fields
        entry1.setField(StandardField.NUMBER, "1");
        entry1.setField(StandardField.NOTE, "some note");
        // unknown fields
        entry1.setField(StandardField.YEAR, "2019");
        entry1.setField(StandardField.CHAPTER, "chapter");

        BibEntry entry2 = new BibEntry(StandardEntryType.Book);
        // required fields
        entry2.setField(StandardField.AUTHOR, "John Book");
        entry2.setField(StandardField.BOOKTITLE, "The Big Book of Books");
        entry2.setField(StandardField.TITLE, "Entry Title");
        entry2.setField(StandardField.DATE, "2017-12-20");

        // optional fields
        entry2.setField(StandardField.NUMBER, "1");
        entry2.setField(StandardField.NOTE, "some note");
        // unknown fields
        entry2.setField(StandardField.YEAR, "2020");
        entry2.setField(StandardField.CHAPTER, "chapter");

        String output = bibEntryWriter.serializeAll(List.of(entry1, entry2), BibDatabaseMode.BIBLATEX);

        // @formatter:off
        String expected1 = """
                @Article{,
                  author       = {Journal Author},
                  date         = {2020-11-16},
                  journaltitle = {Journal of Words},
                  title        = {Entry Title},
                  note         = {some note},
                  number       = {1},
                  chapter      = {chapter},
                  year         = {2019},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        // @formatter:off
        String expected2 = """
                @Book{,
                  author    = {John Book},
                  date      = {2017-12-20},
                  title     = {Entry Title},
                  chapter   = {chapter},
                  note      = {some note},
                  number    = {1},
                  booktitle = {The Big Book of Books},
                  year      = {2020},
                }
                """.replaceAll("\n", OS.NEWLINE);
        // @formatter:on

        assertEquals(expected1 + OS.NEWLINE + expected2, output);
    }

    static Stream<Arguments> testGetFormattedFieldNameData() {
        return Stream.of(
                Arguments.of(" = ", "", 0),
                Arguments.of("a = ", "a", 0),
                Arguments.of("   = ", "", 2),
                Arguments.of("a  = ", "a", 2),
                Arguments.of("abc = ", "abc", 2),
                Arguments.of("abcdef = ", "abcdef", 6)
        );
    }

    @ParameterizedTest
    @MethodSource("testGetFormattedFieldNameData")
    void testGetFormattedFieldName(String expected, String fieldName, int indent) {
        Field field = FieldFactory.parseField(fieldName);
        assertEquals(expected, BibEntryWriter.getFormattedFieldName(field, indent));
    }

    static Stream<Arguments> testGetLengthOfLongestFieldNameData() {
        return Stream.of(
                Arguments.of(1, new BibEntry().withField(FieldFactory.parseField("t"), "t")),
                Arguments.of(5, new BibEntry(EntryTypeFactory.parse("reference"))
                        .withCitationKey("Broecker1984")
                        .withField(StandardField.TITLE, "International Center of Photography}"))
        );
    }

    @ParameterizedTest
    @MethodSource("testGetLengthOfLongestFieldNameData")
    void testGetLengthOfLongestFieldName(int expected, BibEntry entry) {
        assertEquals(expected, BibEntryWriter.getLengthOfLongestFieldName(entry));
    }
}
