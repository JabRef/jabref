package org.jabref.logic.bibtex;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.os.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import io.github.adr.linked.ADR;
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
        FieldPreferences fieldPreferences = new FieldPreferences(true, List.of(StandardField.MONTH), List.of());
        bibEntryWriter = new BibEntryWriter(new FieldWriter(fieldPreferences), new BibEntryTypesManager());
    }

    @Test
    void serialization() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                // set required fields
                .withField(StandardField.AUTHOR, "Foo Bar")
                .withField(StandardField.JOURNAL, "International Journal of Something")
                // set optional fields
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.NOTE, "some note")
                .withChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{,
                  author  = {Foo Bar},
                  journal = {International Journal of Something},
                  note    = {some note},
                  number  = {1},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void bibEntryTwoSpacesBeforeAndAfterKept() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "  two spaces before and after (before)  ")
                .withChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{,
                  author = {  two spaces before and after (before)  },
                }
                """.replace("\n", OS.NEWLINE);

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void bibEntryNotModified() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "  two spaces before and after  ")
                .withChanged(true);

        BibEntry original = new BibEntry(entry);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(original, entry);
    }

    @Test
    void writeOtherTypeTest() throws IOException {
        BibEntry entry = new BibEntry(new UnknownEntryType("other"))
                .withField(StandardField.COMMENT, "testentry")
                .withCitationKey("test")
                .withChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Other{test,
                  comment = {testentry},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeEntryWithFile() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        LinkedFile file = LinkedFile.of("test", Path.of("/home/uers/test.pdf"), "PDF");
        entry.addFile(file);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{,
                  file = {test:/home/uers/test.pdf:PDF},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeEntryWithOrField() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.InBook)
                // set a required OR field (author/editor)
                .withField(StandardField.EDITOR, "Foo Bar")
                .withField(StandardField.JOURNAL, "International Journal of Something")
                // set an optional field
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.NOTE, "some note")
                .withChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @InBook{,
                  editor  = {Foo Bar},
                  note    = {some note},
                  number  = {1},
                  journal = {International Journal of Something},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeEntryWithOrFieldBothFieldsPresent() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.InBook)
                // set a required OR field with both fields(author/editor)
                .withField(StandardField.AUTHOR, "Foo Thor")
                .withField(StandardField.EDITOR, "Edi Bar")
                .withField(StandardField.JOURNAL, "International Journal of Something")
                // set an optional field
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.NOTE, "some note")
                .withChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @InBook{,
                  author  = {Foo Thor},
                  editor  = {Edi Bar},
                  note    = {some note},
                  number  = {1},
                  journal = {International Journal of Something},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeReallyUnknownTypeTest() throws IOException {
        BibEntry entry = new BibEntry(new UnknownEntryType("ReallyUnknownType"))
                .withField(StandardField.COMMENT, "testentry")
                .withCitationKey("test")
                .withChanged(true);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Reallyunknowntype{test,
                  comment = {testentry},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void roundTripTest() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """.replace("\n", OS.NEWLINE);

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripKeepsFilePathWithBackslashes() throws Exception {
        String bibtexEntry = """
                @Article{,
                  file = {Tagungen\\2013\\KWTK45},
                }
                """.replace("\n", OS.NEWLINE);
        final BibEntry entry = firstEntryFrom(bibtexEntry);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripKeepsEscapedCharacters() throws Exception {
        String bibtexEntry = """
                @Article{,
                  demofield = {Tagungen\\2013\\KWTK45},
                }
                """.replace("\n", OS.NEWLINE);

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripKeepsFilePathEndingWithBackslash() throws Exception {
        String bibtexEntry = """
                @Article{,
                  file = {dir\\},
                }
                """.replace("\n", OS.NEWLINE);

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithPrependingNewlines() throws Exception {
        // Keep this as string concatenation; we are testing different line breaks here
        String bibtexEntry = "\r\n@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}" + OS.NEWLINE;

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry.substring(2), stringWriter.toString());
    }

    @Test
    void roundTripWithKeepsCRLFLineBreakStyle() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """.replace("\n", "\r\n");

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        // need to reconfigure writer to use "\r\n"
        bibWriter = new BibWriter(stringWriter, "\r\n");
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithKeepsLFLineBreakStyle() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """;

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        // need to reconfigure writer to use "\n"
        bibWriter = new BibWriter(stringWriter, "\n");
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithModification() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1},
                }
                """.replace("\n", OS.NEWLINE);

        final BibEntry entry = firstEntryFrom(bibtexEntry);

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{test,
                  author  = {BlaBla},
                  journal = {International Journal of Something},
                  note    = {some note},
                  number  = {1},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void roundTripWithCamelCasingInTheOriginalEntryAndResultInLowerCase() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Number                   = {1},
                  Note                     = {some note},
                  HowPublished             = {asdf},
                }
                """.replace("\n", OS.NEWLINE);

        final BibEntry entry = firstEntryFrom(bibtexEntry);

        // modify entry
        entry.setField(StandardField.AUTHOR, "BlaBla");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{test,
                  author       = {BlaBla},
                  journal      = {International Journal of Something},
                  note         = {some note},
                  number       = {1},
                  howpublished = {asdf},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void entryTypeChange() throws Exception {
        String expected = """

                @Article{test,
                  author       = {BlaBla},
                  journal      = {International Journal of Something},
                  number       = {1},
                  note         = {some note},
                  howpublished = {asdf},
                }
                """.replace("\n", OS.NEWLINE);
        final BibEntry entry = firstEntryFrom(expected);

        // modify entry
        entry.setType(StandardEntryType.InProceedings);

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expectedNewEntry = """
                @InProceedings{test,
                  author       = {BlaBla},
                  note         = {some note},
                  number       = {1},
                  howpublished = {asdf},
                  journal      = {International Journal of Something},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expectedNewEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithAppendedNewlines() throws Exception {
        // Keep this as string concatenation; we are testing different line breaks here
        String bibtexEntry = "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Number                   = {1}," + OS.NEWLINE +
                "  Note                     = {some note}" + OS.NEWLINE +
                "}\n\n";

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // Only one appending newline is written by the writer
        // OS.NEWLINE is used, not the given one
        assertEquals(bibtexEntry.substring(0, bibtexEntry.length() - 2) + OS.NEWLINE, actual);
    }

    @Test
    void roundTripNormalizesNewLines() throws Exception {
        // keep this as string concatenation; we are testing MIXED line breaks here
        String bibtexEntry = "@Article{test,\n" +
                "  Author                   = {Foo Bar},\r\n" +
                "  Journal                  = {International Journal of Something},\n" +
                "  Number                   = {1},\n" +
                "  Note                     = {some note}\r\n" +
                "}\n\n";

        final BibEntry entry = firstEntryFrom(bibtexEntry);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        String expected = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Number                   = {1},
                  Note                     = {some note}
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, actual);
    }

    @Test
    void multipleWritesWithoutModification() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """.replace("\n", OS.NEWLINE);

        String result = testSingleWrite(bibtexEntry);
        result = testSingleWrite(result);
        result = testSingleWrite(result);

        assertEquals(bibtexEntry, result);
    }

    private String testSingleWrite(String bibtexEntry) throws Exception {
        final BibEntry entry = firstEntryFrom(bibtexEntry);
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String actual = writer.toString();
        assertEquals(bibtexEntry, actual);
        return actual;
    }

    @Test
    void monthFieldSpecialSyntax() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Month                    = mar,
                  Number                   = {1}
                }
                """.replace("\n", OS.NEWLINE);

        BibEntry entry = firstEntryFrom(bibtexEntry);

        // check month field
        Set<Field> fields = entry.getFields();
        assertTrue(fields.contains(StandardField.MONTH));
        assertTrue(entry.getField(StandardField.MONTH).isPresent());
        assertEquals("#mar#", entry.getField(StandardField.MONTH).get());

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void customTypeCanBewritten() throws Exception {
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
                """.replace("\n", OS.NEWLINE);

        BibEntry entry = firstEntryFrom(bibtexEntry);

        // modify entry
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
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void constantMonthApril() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.MONTH, "#apr#");
        // enable writing
        entry.setChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals("""
                        @Misc{,
                          month = apr,
                        }
                        """.replace("\n", OS.NEWLINE),
                stringWriter.toString());
    }

    @Test
    void monthApril() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.MONTH, "apr");
        // enable writing
        entry.setChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals("""
                        @Misc{,
                          month = {apr},
                        }
                        """.replace("\n", OS.NEWLINE),
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
                """.replace("\n", OS.NEWLINE);

        BibEntry entry = firstEntryFrom(bibtexEntry);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void addFieldWithLongerLength() throws Exception {
        String bibtexEntry = """

                @Article{test,
                  author =  {BlaBla},
                  journal = {International Journal of Something},
                  number =  {1},
                  note =    {some note},
                }""".replace("\n", OS.NEWLINE);
        BibEntry entry = firstEntryFrom(bibtexEntry);

        // modify entry
        entry.setField(StandardField.HOWPUBLISHED, "asdf");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{test,
                  author       = {BlaBla},
                  journal      = {International Journal of Something},
                  note         = {some note},
                  number       = {1},
                  howpublished = {asdf},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void doNotWriteEmptyFields() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "  ")
                .withField(StandardField.NOTE, "some note")
                .withChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{,
                  note   = {some note},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void writeThrowsErrorIfFieldContainsUnbalancedBraces() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.NOTE, "some text with unbalanced { braces")
                .withChanged(true);

        assertThrows(IOException.class, () -> bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX));
    }

    @Test
    void roundTripWithPrecedingCommentTest() throws Exception {
        String bibtexEntry = """
                % Some random comment that should stay here
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }
                """.replace("\n", OS.NEWLINE);

        BibEntry entry = firstEntryFrom(bibtexEntry);

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        assertEquals(bibtexEntry, stringWriter.toString());
    }

    @Test
    void roundTripWithPrecedingCommentAndModificationTest() throws Exception {
        String bibtexEntry = """
                % Some random comment that should stay here
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Number                   = {1},
                  Note                     = {some note}
                }
                """.replace("\n", OS.NEWLINE);

        BibEntry entry = firstEntryFrom(bibtexEntry);

        // modify entry
        entry.setField(StandardField.AUTHOR, "John Doe");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                % Some random comment that should stay here
                @Article{test,
                  author  = {John Doe},
                  journal = {International Journal of Something},
                  note    = {some note},
                  number  = {1},
                }
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void alphabeticSerialization() throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                // required fields
                .withField(StandardField.AUTHOR, "Foo Bar")
                .withField(StandardField.JOURNALTITLE, "International Journal of Something")
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.DATE, "2019-10-16")
                // optional fields
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.NOTE, "some note")
                // unknown fields
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.CHAPTER, "chapter")
                .withChanged(true);

        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBLATEX);

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
                """.replace("\n", OS.NEWLINE);
        assertEquals(expected, stringWriter.toString());
    }

    @Test
    void serializeAll() throws IOException {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                // required fields
                .withField(StandardField.AUTHOR, "Journal Author")
                .withField(StandardField.JOURNALTITLE, "Journal of Words")
                .withField(StandardField.TITLE, "Entry Title")
                .withField(StandardField.DATE, "2020-11-16")

                // optional fields
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.NOTE, "some note")
                // unknown fields
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.CHAPTER, "chapter")
                .withChanged(true);

        BibEntry entry2 = new BibEntry(StandardEntryType.Book)
                // required fields
                .withField(StandardField.AUTHOR, "John Book")
                .withField(StandardField.BOOKTITLE, "The Big Book of Books")
                .withField(StandardField.TITLE, "Entry Title")
                .withField(StandardField.DATE, "2017-12-20")

                // optional fields
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.NOTE, "some note")
                // unknown fields
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.CHAPTER, "chapter")
                .withChanged(true);

        String output = bibEntryWriter.serializeAll(List.of(entry1, entry2), BibDatabaseMode.BIBLATEX);

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
                """.replace("\n", OS.NEWLINE);

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
                """.replace("\n", OS.NEWLINE);

        assertEquals(expected1 + OS.NEWLINE + expected2, output);
    }

    static Stream<Arguments> getFormattedFieldName() {
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
    @MethodSource
    void getFormattedFieldName(String expected, String fieldName, int indent) {
        Field field = FieldFactory.parseField(fieldName);
        assertEquals(expected, BibEntryWriter.getFormattedFieldName(field, indent));
    }

    static Stream<Arguments> getLengthOfLongestFieldName() {
        return Stream.of(
                Arguments.of(1, new BibEntry().withField(FieldFactory.parseField("t"), "t")),
                Arguments.of(5, new BibEntry(EntryTypeFactory.parse("reference"))
                        .withCitationKey("Broecker1984")
                        .withField(StandardField.TITLE, "International Center of Photography}"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void getLengthOfLongestFieldName(int expected, BibEntry entry) {
        assertEquals(expected, BibEntryWriter.getLengthOfLongestFieldName(entry));
    }

    /**
     * Provides the first entry, from the database, built of the given textual representation.
     * <p>
     * Instance import preferences object used.
     */
    private BibEntry firstEntryFrom(final String bibContentText) throws JabRefException {
        return BibDatabaseContext
                .of(bibContentText, importFormatPreferences)
                .getEntries()
                .getFirst();
    }

    @ADR(49)
    @Test
    void lowercaseStandardAndPreserveCustomCasing() throws Exception {
        String bibtexEntry = """
                @Article{test,
                  Author                   = {Foo Bar},
                  Title                    = {My title},
                  CustomField              = {Some value}
                }
                """.replace("\n", OS.NEWLINE);

        BibEntry entry = firstEntryFrom(bibtexEntry);

        // modify entry
        entry.setField(new UnknownField("CustomField"), "Some other value");

        // write out bibtex string
        bibEntryWriter.write(entry, bibWriter, BibDatabaseMode.BIBTEX);

        String expected = """
                @Article{test,
                  author      = {Foo Bar},
                  title       = {My title},
                  CustomField = {Some other value},
                }
                """.replace("\n", OS.NEWLINE);

        assertEquals(expected, stringWriter.toString());
    }
}
