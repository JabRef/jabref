package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.DatabaseCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.formatter.bibtexfields.EscapeAmpersandsFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeDollarSignFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeUnderscoresFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.os.OS;
import org.jabref.model.TreeNode;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for reading whole bib files can be found at {@link org.jabref.logic.importer.fileformat.BibtexImporterTest}
 * <p>
 * Tests cannot be executed concurrently, because Localization is used at {@link BibtexParser#parseAndAddEntry(String)}
 */
class BibtexParserTest {
    private static final String BIB_DESK_ROOT_GROUP_NAME = "BibDeskGroups";
    private ImportFormatPreferences importFormatPreferences;
    private BibtexParser parser;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        parser = new BibtexParser(importFormatPreferences);
    }

    @Test
    void parseWithNullThrowsNullPointerException() {
        Executable toBeTested = () -> parser.parse(null);
        assertThrows(NullPointerException.class, toBeTested);
    }

    @Test
    void fromStringRecognizesEntry() throws ParseException {
        List<BibEntry> result = parser
                .parseEntries("@article{test,author={Ed von Test}}");
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result);
    }

    @Test
    void fromStringReturnsEmptyListFromEmptyString() throws ParseException {
        Collection<BibEntry> parsed = parser.parseEntries("");
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    void fromStringReturnsEmptyListIfNoEntryRecognized() throws ParseException {
        Collection<BibEntry> parsed = parser
                .parseEntries("@@article@@{{{{{{}");
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    void singleFromStringRecognizesEntry() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString(
                """
                        @article{canh05,  author = {Crowston, K. and Annabi, H.},
                          title = {Title A}}
                        """,
                importFormatPreferences);
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("canh05")
                .withField(StandardField.AUTHOR, "Crowston, K. and Annabi, H.")
                .withField(StandardField.TITLE, "Title A");
        assertEquals(Optional.of(expected), parsed);
    }

    @Test
    void singleFromStringRecognizesEntryInMultiple() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString("""
                        @article{canh05, author = {Crowston, K. and Annabi, H.},
                            title = {Title A}}
                        @inProceedings{foo,  author={Norton Bar}}""",
                importFormatPreferences);
        assertTrue(parsed.get().getCitationKey().equals(Optional.of("canh05"))
                || parsed.get().getCitationKey().equals(Optional.of("foo")));
    }

    @Test
    void singleFromStringReturnsEmptyFromEmptyString() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString("", importFormatPreferences);
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void singleFromStringReturnsEmptyIfNoEntryRecognized() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString("@@article@@{{{{{{}", importFormatPreferences);
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void parseRecognizesEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesFieldValuesInQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Ed von Test\"}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryOnlyWithKey() throws IOException {
        ParserResult result = parser.parse(new StringReader("@article{test}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWithWhitespaceAtBeginning() throws IOException {
        ParserResult result = parser
                .parse(new StringReader(" @article{test,author={Ed von Test}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withUserComments(" ")
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWithWhitespace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article { test,author={Ed von Test}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWithNewlines() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article\n{\ntest,author={Ed von Test}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWithUnknownType() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@unknown{test,author={Ed von Test}}"));
        BibEntry expected = new BibEntry(new UnknownEntryType("unknown"))
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWithVeryLongType() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@thisIsALongStringToTestMaybeItIsToLongWhoKnowsNOTme{test,author={Ed von Test}}"));
        BibEntry expected = new BibEntry(new UnknownEntryType("thisisalongstringtotestmaybeitistolongwhoknowsnotme"))
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryInParenthesis() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article(test,author={Ed von Test})"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWithBigNumbers() throws IOException {
        ParserResult result = parser.parse(new StringReader("""
                @article{canh05,isbn = 1234567890123456789,
                isbn2 = {1234567890123456789},
                small = 1234,
                }"""));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("canh05")
                .withField(StandardField.ISBN, "1234567890123456789")
                .withField(new UnknownField("isbn2"), "1234567890123456789")
                .withField(new UnknownField("small"), "1234");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesCitationKeyWithSpecialCharacters() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{te_st:with-special(characters),author={Ed von Test}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("te_st:with-special(characters)")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWhereLastFieldIsFinishedWithComma() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test},}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryWithAtInField() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von T@st}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(InternalField.KEY_FIELD, "test")
                .withField(StandardField.AUTHOR, "Ed von T@st");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesEntryPrecedingComment() throws IOException {
        String comment = "@Comment{@article{myarticle,}" + OS.NEWLINE
                + "@inproceedings{blabla, title={the proceedings of bl@bl@}; }" + OS.NEWLINE + "}" + OS.NEWLINE;
        String entryWithComment = comment + "@article{test,author={Ed von T@st}}";
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(InternalField.KEY_FIELD, "test")
                .withField(StandardField.AUTHOR, "Ed von T@st");
        expected.setCommentsBeforeEntry(comment);

        ParserResult result = parser.parse(new StringReader(entryWithComment));
        List<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(List.of(expected), parsed);
        assertEquals(expected.getUserComments(), parsed.getFirst().getUserComments());
    }

    @Test
    void parseRecognizesMultipleEntries() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("""
                        @article{canh05,  author = {Crowston, K. and Annabi, H.},
                          title = {Title A}}
                        @inProceedings{foo,  author={Norton Bar}}"""));
        List<BibEntry> expected = List.of(
                new BibEntry(StandardEntryType.Article)
                        .withCitationKey("canh05")
                        .withField(StandardField.AUTHOR, "Crowston, K. and Annabi, H.")
                        .withField(StandardField.TITLE, "Title A"),
                new BibEntry(StandardEntryType.InProceedings)
                        .withCitationKey("foo")
                        .withField(StandardField.AUTHOR, "Norton Bar"));
        assertEquals(expected, result.getDatabase().getEntries());
    }

    @Test
    void parseSetsParsedSerialization() throws IOException {
        String firstEntry = "@article{canh05," + "  author = {Crowston, K. and Annabi, H.}," + OS.NEWLINE
                + "  title = {Title A}}" + OS.NEWLINE;
        String secondEntry = "@inProceedings{foo," + "  author={Norton Bar}}";
        List<BibEntry> parsedEntries = parser.parse(new StringReader(firstEntry + secondEntry))
                                             .getDatabase().getEntries();
        assertEquals(firstEntry, parsedEntries.getFirst().getParsedSerialization());
        assertEquals(secondEntry, parsedEntries.get(1).getParsedSerialization());
    }

    @Test
    void parseRecognizesMultipleEntriesOnSameLine() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{canh05}" + "@inProceedings{foo}"));
        List<BibEntry> expected = List.of(
                new BibEntry(StandardEntryType.Article)
                        .withCitationKey("canh05"),
                new BibEntry(StandardEntryType.InProceedings)
                        .withCitationKey("foo"));
        assertEquals(expected, result.getDatabase().getEntries());
    }

    @Test
    void parseCombinesMultipleAuthorFields() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@article{test,author={Ed von Test},author={Second Author},author={Third Author}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test and Second Author and Third Author");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseCombinesMultipleEditorFields() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@article{test,editor={Ed von Test},editor={Second Author},editor={Third Author}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.EDITOR, "Ed von Test and Second Author and Third Author");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseCombinesMultipleKeywordsFields() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@article{test,Keywords={Test},Keywords={Second Keyword},Keywords={Third Keyword}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.KEYWORDS, "Test, Second Keyword, Third Keyword");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesHeaderButIgnoresEncoding() throws IOException {
        ParserResult result = parser.parse(new StringReader("""
                This file was created with JabRef 2.1 beta 2.
                Encoding: Cp1252

                @INPROCEEDINGS{CroAnnHow05,
                  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},
                  title = {Effective work practices for floss development: A model and propositions},
                  booktitle = {Hawaii International Conference On System Sciences (HICSS)},
                  year = {2005},
                  owner = {oezbek},
                  timestamp = {2006.05.29},
                  url = {http://james.howison.name/publications.html}
                }))"""));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(Optional.empty(), result.getMetaData().getEncoding());
        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.InProceedings, entry.getType());
        assertEquals(8, entry.getFields().size());
        assertEquals(Optional.of("CroAnnHow05"), entry.getCitationKey());
        assertEquals(Optional.of("Crowston, K. and Annabi, H. and Howison, J. and Masango, C."), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Effective work practices for floss development: A model and propositions"),
                entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Hawaii International Conference On System Sciences (HICSS)"),
                entry.getField(StandardField.BOOKTITLE));
        assertEquals(Optional.of("2005"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("oezbek"), entry.getField(StandardField.OWNER));
        assertEquals(Optional.of("2006.05.29"), entry.getField(StandardField.TIMESTAMP));
        assertEquals(Optional.of("http://james.howison.name/publications.html"), entry.getField(StandardField.URL));
    }

    @Test
    void parseRecognizesFormatedEntry() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("""
                        @INPROCEEDINGS{CroAnnHow05,
                          author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},
                          title = {Effective work practices for floss development: A model and propositions},
                          booktitle = {Hawaii International Conference On System Sciences (HICSS)},
                          year = {2005},
                          owner = {oezbek},
                          timestamp = {2006.05.29},
                          url = {http://james.howison.name/publications.html}
                        }))"""));

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("CroAnnHow05")
                .withField(StandardField.AUTHOR, "Crowston, K. and Annabi, H. and Howison, J. and Masango, C.")
                .withField(StandardField.TITLE, "Effective work practices for floss development: A model and propositions")
                .withField(StandardField.BOOKTITLE, "Hawaii International Conference On System Sciences (HICSS)")
                .withField(StandardField.YEAR, "2005")
                .withField(StandardField.OWNER, "oezbek")
                .withField(StandardField.TIMESTAMP, "2006.05.29")
                .withField(StandardField.URL, "http://james.howison.name/publications.html");

        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesNumbersWithoutBracketsOrQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,year = 2005}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.YEAR, "2005");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesUppercaseFields() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,AUTHOR={Ed von Test}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesAbsoluteFile() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,file = {D:\\Documents\\literature\\Tansel-PRL2006.pdf}}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.FILE, "D:\\Documents\\literature\\Tansel-PRL2006.pdf");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesFinalSlashAsSlash() throws Exception {
        ParserResult result = parser
                .parse(new StringReader("""
                        @misc{,
                          test = {wired\\},
                        }
                        """));
        assertEquals(
                List.of(new BibEntry()
                        .withField(new UnknownField("test"), "wired\\")),
                result.getDatabase().getEntries()
        );
    }

    /**
     * JabRef's heuristics is not able to parse this special case.
     */
    @Test
    void parseFailsWithFinalSlashAsSlashWhenSingleLine() throws Exception {
        ParserResult parserResult = parser.parse(new StringReader("@misc{, test = {wired\\}}"));
        // In case JabRef was more relaxed, `assertFalse` would be provided here.
        assertTrue(parserResult.hasWarnings());
    }

    @Test
    void parseRecognizesDateFieldWithConcatenation() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,date = {1-4~} # nov}"));
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.DATE, "1-4~#nov#");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseReturnsEmptyListIfNoEntryRecognized() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("""
                          author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},
                          title = {Effective work practices for floss development: A model and propositions},
                          booktitle = {Hawaii International Conference On System Sciences (HICSS)},
                          year = {2005},
                          owner = {oezbek},
                          timestamp = {2006.05.29},
                          url = {http://james.howison.name/publications.html}
                        }))"""));
        assertTrue(result.hasWarnings());
        assertEquals(List.of(), result.getDatabase().getEntries());
    }

    @Test
    void parseReturnsEmptyListIfNoEntryExistent() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("""
                        This was created with JabRef 2.1 beta 2.
                        Encoding: Cp1252
                        """));
        assertEquals(List.of(), result.getDatabase().getEntries());
    }

    @Test
    void parseNotWarnsAboutEntryWithoutCitationKey() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{,author={Ed von Test}}"));
        assertFalse(result.hasWarnings());
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Ed von Test");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracket() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={author missing bracket}"));
        assertTrue(result.hasWarnings());
        assertEquals(List.of(), result.getDatabase().getEntries());
    }

    @Test
    void parseAddsEscapedOpenBracketToFieldValue() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,review={escaped \\{ bracket}}"));
        assertFalse(result.hasWarnings());

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.REVIEW, "escaped \\{ bracket");
        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseAddsEscapedClosingBracketToFieldValue() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,review={escaped \\} bracket}}"));

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.REVIEW, "escaped \\} bracket");

        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracketInQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"author {missing bracket\"}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(0, parsed.size());
        assertTrue(result.hasWarnings());
    }

    @Test
    void parseMovesArbitraryContentAfterEntryToEpilog() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={author bracket }}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(1, parsed.size(), "Size should be one, but was " + parsed.size());
        assertEquals("}", result.getDatabase().getEpilog(), "Epilog should be preserved");
    }

    @Test
    void parseWarnsAboutUnmatchedContentInEntryWithoutComma() throws IOException {
        ParserResult result = parser.parse(new StringReader("@article{test,author={author bracket } too much}"));

        List<BibEntry> entries = result.getDatabase().getEntries();

        assertEquals(Optional.of("author bracket #too##much#"), entries.getFirst().getField(StandardField.AUTHOR));
    }

    @Test
    void parseWarnsAboutUnmatchedContentInEntry() throws IOException {
        ParserResult result = parser.parse(new StringReader("@article{test,author={author bracket }, too much}"));

        List<BibEntry> entries = result.getDatabase().getEntries();

        assertTrue(result.hasWarnings(), "There should be warnings");
        assertEquals(0, entries.size(), "Size should be zero, but was " + entries.size());
    }

    @Test
    void parseAcceptsEntryWithAtSymbolInBrackets() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={author @ good}}"));

        List<BibEntry> entries = result.getDatabase().getEntries();

        assertEquals(1, entries.size());
        assertEquals(Optional.of("author @ good"), entries.getFirst().getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithAtSymbolInQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"author @ good\"}"));

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "author @ good");

        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesFieldsWithBracketsEnclosedInQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Test {Ed {von} Test}\"}"));

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test {Ed {von} Test}");

        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesFieldsWithEscapedQuotationMarks() throws IOException {
        // Quotes in fields of the form key = "value" have to be escaped by putting them into braces
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Test {\" Test}\"}"));

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test {\" Test}");

        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseRecognizesFieldsWithQuotationMarksInBrackets() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,title=\"Comments on {\"}Filenames and Fonts{\"}\"}"));

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.TITLE, "Comments on {\"}Filenames and Fonts{\"}");

        assertEquals(List.of(expected), result.getDatabase().getEntries());
    }

    @Test
    void parseIgnoresAndWarnsAboutEntryWithFieldsThatAreNotSeperatedByComma() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test} year=2005}"));

        assertEquals(List.of(), result.getDatabase().getEntries());
        assertTrue(result.hasWarnings());
    }

    @Test
    void parseIgnoresAndWarnsAboutCorruptedEntryButRecognizeOthers() throws IOException {
        ParserResult result = parser.parse(
                new StringReader(
                        "@article{test,author={author missing bracket}" + "@article{test,author={Ed von Test}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
        assertTrue(result.hasWarnings());
    }

    @Test
    void parseRecognizesMonthFieldsWithFollowingComma() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test},month={8,}},"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(3, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("8,"), entry.getField(StandardField.MONTH));
    }

    @Test
    void parseRecognizesPreamble() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@preamble{some text and \\latex}"));

        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    void parseRecognizesUppercasePreamble() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@PREAMBLE{some text and \\latex}"));

        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    void parseRecognizesPreambleWithWhitespace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@preamble {some text and \\latex}"));

        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    void parseRecognizesPreambleInParenthesis() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@preamble(some text and \\latex)"));

        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    void parseRecognizesPreambleWithConcatenation() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@preamble{\"some text\" # \"and \\latex\"}"));

        assertEquals(Optional.of("\"some text\" # \"and \\latex\""), result.getDatabase().getPreamble());
    }

    @Test
    void parseRecognizesString() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}"));

        BibtexString string = result.getDatabase().getStringValues().iterator().next();

        assertEquals(1, result.getDatabase().getStringCount());
        assertEquals("bourdieu", string.getName());
        assertEquals("Bourdieu, Pierre", string.getContent());
    }

    @Test
    void parseRecognizesStringWithQuotes() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@string{bourdieu = \"Bourdieu, Pierre\"}"));

        BibtexString string = result.getDatabase().getStringValues().iterator().next();

        assertEquals(1, result.getDatabase().getStringCount());
        assertEquals("bourdieu", string.getName());
        assertEquals("Bourdieu, Pierre", string.getContent());
    }

    @Test
    void parseSavesOneNewlineAfterStringInParsedSerialization() throws IOException {
        String string = "@string{bourdieu = {Bourdieu, Pierre}}" + OS.NEWLINE;
        ParserResult result = parser
                .parse(new StringReader(string + OS.NEWLINE + OS.NEWLINE));

        BibtexString parsedString = result.getDatabase().getStringValues().iterator().next();

        assertEquals(1, result.getDatabase().getStringCount());
        assertEquals(string, parsedString.getParsedSerialization());
    }

    @Test
    void parseRecognizesStringWithWhitespace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@string {bourdieu = {Bourdieu, Pierre}}"));

        BibtexString parsedString = result.getDatabase().getStringValues().iterator().next();

        assertEquals(1, result.getDatabase().getStringCount());
        assertEquals("bourdieu", parsedString.getName());
        assertEquals("Bourdieu, Pierre", parsedString.getContent());
    }

    @Test
    void parseRecognizesStringInParenthesis() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@string(bourdieu = {Bourdieu, Pierre})"));

        BibtexString parsedString = result.getDatabase().getStringValues().iterator().next();

        assertEquals(1, result.getDatabase().getStringCount());
        assertEquals("bourdieu", parsedString.getName());
        assertEquals("Bourdieu, Pierre", parsedString.getContent());
    }

    @Test
    void parseRecognizesMultipleStrings() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}" + "@string{adieu = {Adieu, Pierre}}"));

        Iterator<BibtexString> iterator = result.getDatabase().getStringValues().iterator();
        BibtexString first = iterator.next();
        BibtexString second = iterator.next();
        // Sort them because we can't be sure about the order
        if ("adieu".equals(first.getName())) {
            BibtexString tmp = first;
            first = second;
            second = tmp;
        }

        assertEquals(2, result.getDatabase().getStringCount());
        assertEquals("bourdieu", first.getName());
        assertEquals("Bourdieu, Pierre", first.getContent());
        assertEquals("adieu", second.getName());
        assertEquals("Adieu, Pierre", second.getContent());
    }

    @Test
    void parseRecognizesStringAndEntry() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@string{bourdieu = {Bourdieu, Pierre}}"
                        + "@book{bourdieu-2002-questions-sociologie, " + "    Address = {Paris}," + "    Author = bourdieu,"
                        + "    Isbn = 2707318256," + "    Publisher = {Minuit}," + "    Title = {Questions de sociologie},"
                        + "    Year = 2002" + "}"));

        BibtexString parsedString = result.getDatabase().getStringValues().iterator().next();
        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, result.getDatabase().getStringCount());
        assertEquals("bourdieu", parsedString.getName());
        assertEquals("Bourdieu, Pierre", parsedString.getContent());
        assertEquals(1, parsedEntries.size());
        assertEquals(StandardEntryType.Book, parsedEntry.getType());
        assertEquals(Optional.of("bourdieu-2002-questions-sociologie"), parsedEntry.getCitationKey());
        assertEquals(Optional.of("Paris"), parsedEntry.getField(StandardField.ADDRESS));
        assertEquals(Optional.of("#bourdieu#"), parsedEntry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("2707318256"), parsedEntry.getField(StandardField.ISBN));
        assertEquals(Optional.of("Minuit"), parsedEntry.getField(StandardField.PUBLISHER));
        assertEquals(Optional.of("Questions de sociologie"), parsedEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2002"), parsedEntry.getField(StandardField.YEAR));
    }

    @Test
    void parseWarnsAboutStringsWithSameNameAndOnlyKeepsOne() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}" + "@string{bourdieu = {Other}}"));

        assertTrue(result.hasWarnings());
        assertEquals(1, result.getDatabase().getStringCount());
    }

    @Test
    void parseIgnoresComments() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{some text and \\latex}"));

        assertEquals(List.of(), result.getDatabase().getEntries());
    }

    // TODO: We should keep @comment if it is the only "thing" in the file
    @Test
    void parseIgnoresUppercaseComments() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@COMMENT{some text and \\latex}"));
        assertFalse(result.hasWarnings()); // FIXME: We silently remove @COMMENT
        assertEquals(List.of(), result.getDatabase().getEntries());
    }

    @Test
    void parseKeepsCommentsAsUserComments() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{some text and \\latex}" + "@article{test,author={Ed von Test}}"));

        assertEquals(List.of(new BibEntry(StandardEntryType.Article)
                        .withCitationKey("test")
                        .withField(StandardField.AUTHOR, "Ed von Test")
                        .withUserComments("@comment{some text and \\latex}")),
                result.getDatabase().getEntries());
    }

    @Test
    void parseIgnoresCommentsAfterEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "@comment{some text and \\latex}"));

        assertEquals(List.of(new BibEntry(StandardEntryType.Article)
                        .withCitationKey("test")
                        .withField(StandardField.AUTHOR, "Ed von Test")),
                result.getDatabase().getEntries());
        assertEquals("@comment{some text and \\latex}", result.getDatabase().getEpilog());
    }

    @Test
    void parseIgnoresText() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("comment{some text and \\latex"));

        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    void parseIgnoresTextBeforeEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("comment{some text and \\latex" + "@article{test,author={Ed von Test}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(StandardEntryType.Article, parsedEntry.getType());
        assertEquals(Optional.of("test"), parsedEntry.getCitationKey());
        assertEquals(2, parsedEntry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), parsedEntry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseIgnoresTextAfterEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "comment{some text and \\latex"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(StandardEntryType.Article, parsedEntry.getType());
        assertEquals(Optional.of("test"), parsedEntry.getCitationKey());
        assertEquals(2, parsedEntry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), parsedEntry.getField(StandardField.AUTHOR));
    }

    @Test
    void parsKeesNewlines() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\nb}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a\nb"), parsedEntry.getField(new UnknownField("a")));
    }

    @Test
    void parsKeepsMultipleNewlines() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("""
                        @article{test,a = {a

                        b},b = {a
                        \s
                        b},c = {a\s
                        \s
                         b}}"""));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a\n\nb"), parsedEntry.getField(new UnknownField("a")));
        assertEquals(Optional.of("a\n \nb"), parsedEntry.getField(new UnknownField("b")));
        assertEquals(Optional.of("a \n \n b"), parsedEntry.getField(new UnknownField("c")));
    }

    @Test
    void parseKeepsTabs() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\tb}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a\tb"), parsedEntry.getField(new UnknownField("a")));
    }

    @Test
    void parsKeepsMultipleTabs() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\t\tb}," + "b = {a\t \tb}," + "c = {a \t \t b}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a\t\tb"), parsedEntry.getField(new UnknownField("a")));
        assertEquals(Optional.of("a\t \tb"), parsedEntry.getField(new UnknownField("b")));
        assertEquals(Optional.of("a \t \t b"), parsedEntry.getField(new UnknownField("c")));
    }

    @Test
    void parsePreservesMultipleSpacesInNonWrappableField() throws IOException {
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(
                FXCollections.observableArrayList(List.of(StandardField.FILE)));
        BibtexParser parser = new BibtexParser(importFormatPreferences);
        ParserResult result = parser
                .parse(new StringReader("@article{canh05,file = {ups  sala}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("ups  sala"), parsedEntry.getField(StandardField.FILE));
    }

    @Test
    void parsePreservesTabsInAbstractField() throws IOException {
        ParserResult result = parser.parse(new StringReader("@article{canh05,abstract = {ups  \tsala}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("ups  \tsala"), parsedEntry.getField(StandardField.ABSTRACT));
    }

    @Test
    void parsePreservesNewlineInAbstractField() throws IOException {
        ParserResult result = parser.parse(new StringReader("@article{canh05,abstract = {ups \nsala}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("ups \nsala"), parsedEntry.getField(StandardField.ABSTRACT));
    }

    @Test
    void parseHandlesAccentsCorrectly() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author = {H'{e}lne Fiaux}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertFalse(result.hasWarnings());
        assertEquals(1, parsedEntries.size());
        assertEquals(StandardEntryType.Article, parsedEntry.getType());
        assertEquals(Optional.of("test"), parsedEntry.getCitationKey());
        assertEquals(Optional.of("H'{e}lne Fiaux"), parsedEntry.getField(StandardField.AUTHOR));
    }

    /**
     * Test for <a href="https://github.com/JabRef/jabref/issues/669">#669</a>
     */
    @Test
    void parsePreambleAndEntryWithoutNewLine() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@preamble{some text and \\latex}@article{test,author = {H'{e}lne Fiaux}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertFalse(result.hasWarnings());
        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
        assertEquals(1, parsedEntries.size());
        assertEquals(StandardEntryType.Article, parsedEntry.getType());
        assertEquals(Optional.of("test"), parsedEntry.getCitationKey());
        assertEquals(Optional.of("H'{e}lne Fiaux"), parsedEntry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseFileHeaderAndPreambleWithoutNewLine() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("\\% Encoding: US-ASCII@preamble{some text and \\latex}"));

        assertFalse(result.hasWarnings());
        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    void parseSavesEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser.parse(new StringReader(testEntry));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(testEntry, parsedEntry.getParsedSerialization());
    }

    @Test
    void parseSavesOneNewlineAfterEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntry + OS.NEWLINE + OS.NEWLINE));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(testEntry + OS.NEWLINE, parsedEntry.getParsedSerialization());
    }

    @Test
    void parseSavesAllButOneNewlinesBeforeEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntry));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        // The first newline is removed, because JabRef interprets that always as block separator
        assertEquals(OS.NEWLINE + OS.NEWLINE + testEntry, parsedEntry.getParsedSerialization());
    }

    @Test
    void parseRemovesEncodingLineAndSeparatorInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser.parse(
                new StringReader(SaveConfiguration.ENCODING_PREFIX + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntry));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        // First two newlines are removed because of removal of "Encoding"
        // Third newline removed because of block functionality
        assertEquals(testEntry, parsedEntry.getParsedSerialization());
    }

    @Test
    void parseSavesNewlinesBetweenEntriesInParsedSerialization() throws IOException {
        String testEntryOne = "@article{test1,author={Ed von Test}}";
        String testEntryTwo = "@article{test2,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntryOne + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntryTwo));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        Iterator<BibEntry> iterator = parsedEntries.iterator();
        BibEntry first = iterator.next();
        BibEntry second = iterator.next();
        // Sort them because we can't be sure about the order
        if (first.getCitationKey().equals(Optional.of("test2"))) {
            BibEntry tmp = first;
            first = second;
            second = tmp;
        }

        assertEquals(2, parsedEntries.size());
        assertEquals(testEntryOne + OS.NEWLINE, first.getParsedSerialization());
        // one newline is removed, because it is written by JabRef's block functionality
        assertEquals(OS.NEWLINE + testEntryTwo, second.getParsedSerialization());
    }

    @Test
    void parseIgnoresWhitespaceInEpilogue() throws IOException {
        ParserResult result = parser.parse(new StringReader("   " + OS.NEWLINE));

        assertEquals("", result.getDatabase().getEpilog());
    }

    @Test
    void parseIgnoresWhitespaceInEpilogueAfterEntry() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntry + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + "  " + OS.NEWLINE));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(testEntry + OS.NEWLINE, parsedEntry.getParsedSerialization());
        assertEquals("", result.getDatabase().getEpilog());
    }

    @Test
    void parseTrimsWhitespaceInEpilogueAfterEntry() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntry + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + " epilogue " + OS.NEWLINE));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(testEntry + OS.NEWLINE, parsedEntry.getParsedSerialization());
        assertEquals("epilogue", result.getDatabase().getEpilog());
    }

    @Test
    void parseRecognizesSaveActionsAfterEntry() throws IOException {
        ParserResult parserResult = parser.parse(
                new StringReader("""
                        @InProceedings{6055279,
                          Title                    = {Educational session 1},
                          Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},
                          Year                     = {2011},
                          Month                    = {Sept},
                          Pages                    = {1-7},
                          Abstract                 = {Start of the above-titled section of the conference proceedings record.},
                          DOI                      = {10.1109/CICC.2011.6055279},
                          ISSN                     = {0886-5930}
                        }

                        @comment{jabref-meta: saveActions:enabled;title[lower_case]}"""));

        FieldFormatterCleanups saveActions = parserResult.getMetaData().getSaveActions().get();

        assertTrue(saveActions.isEnabled());
        assertEquals(List.of(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())),
                saveActions.getConfiguredActions());
    }

    @Test
    void parserKeepsSaveActions() throws IOException {
        ParserResult parserResult = parser.parse(
                new StringReader("""
                        @InProceedings{6055279,
                          Title                    = {Educational session 1},
                          Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},
                          Year                     = {2011},
                          Month                    = {Sept},
                          Pages                    = {1-7},
                          Abstract                 = {Start of the above-titled section of the conference proceedings record.},
                          DOI                      = {10.1109/CICC.2011.6055279},
                          ISSN                     = {0886-5930}
                        }

                        @Comment{jabref-meta: saveActions:enabled;
                        month[normalize_month]
                        pages[normalize_page_numbers]
                        title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                        booktitle[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                        publisher[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                        journal[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                        abstract[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                        ;}
                        """));

        FieldFormatterCleanups saveActions = parserResult.getMetaData().getSaveActions().get();

        assertTrue(saveActions.isEnabled());

        List<FieldFormatterCleanup> expected = new ArrayList<>(30);
        expected.add(new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()));
        expected.add(new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()));
        for (Field field : List.of(StandardField.TITLE, StandardField.BOOKTITLE, StandardField.PUBLISHER, StandardField.JOURNAL, StandardField.ABSTRACT)) {
            expected.add(new FieldFormatterCleanup(field, new EscapeAmpersandsFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeDollarSignFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeUnderscoresFormatter()));
            expected.add(new FieldFormatterCleanup(field, new LatexCleanupFormatter()));
        }
        assertEquals(expected, saveActions.getConfiguredActions());
    }

    @Test
    void parseRecognizesCRLFLineBreak() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("""
                        @InProceedings{6055279,\r
                          Title                    = {Educational session 1},\r
                          Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},\r
                          Year                     = {2011},\r
                          Month                    = {Sept},\r
                          Pages                    = {1-7},\r
                          Abstract                 = {Start of the above-titled section of the conference proceedings record.},\r
                          DOI                      = {10.1109/CICC.2011.6055279},\r
                          ISSN                     = {0886-5930}\r
                        }\r
                        """));
        assertEquals("\r\n", result.getDatabase().getNewLineSeparator());
    }

    @Test
    void parseRecognizesLFLineBreak() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("""
                        @InProceedings{6055279,
                          Title                    = {Educational session 1},
                          Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},
                          Year                     = {2011},
                          Month                    = {Sept},
                          Pages                    = {1-7},
                          Abstract                 = {Start of the above-titled section of the conference proceedings record.},
                          DOI                      = {10.1109/CICC.2011.6055279},
                          ISSN                     = {0886-5930}
                        }
                        """));
        assertEquals("\n", result.getDatabase().getNewLineSeparator());
    }

    @Test
    void integrationTestSaveActions() throws IOException {
        ParserResult parserResult = parser
                .parse(new StringReader("@comment{jabref-meta: saveActions:enabled;title[lower_case]}"));

        FieldFormatterCleanups saveActions = parserResult.getMetaData().getSaveActions().get();

        assertTrue(saveActions.isEnabled());
        assertEquals(List.of(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())),
                saveActions.getConfiguredActions());
    }

    @Test
    void integrationTestBibEntryType() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@comment{jabref-entrytype: Lecturenotes: req[author;title] opt[language;url]}"));

        BibEntryType expectedEntryType = new BibEntryType(
                new UnknownEntryType("lecturenotes"),
                Arrays.asList(
                        new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT),
                        new BibField(StandardField.TITLE, FieldPriority.IMPORTANT),
                        new BibField(StandardField.LANGUAGE, FieldPriority.IMPORTANT),
                        new BibField(StandardField.URL, FieldPriority.IMPORTANT)),
                Arrays.asList(
                        new OrFields(StandardField.AUTHOR),
                        new OrFields(StandardField.TITLE)
                ));

        assertEquals(Collections.singleton(expectedEntryType), result.getEntryTypes());
    }

    @Test
    void integrationTestSaveOrderConfig() throws IOException {
        ParserResult result = parser.parse(
                new StringReader(
                        "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"));

        Optional<SaveOrder> saveOrderConfig = result.getMetaData().getSaveOrder();

        assertEquals(new SaveOrder(SaveOrder.OrderType.SPECIFIED, List.of(
                        new SaveOrder.SortCriterion(StandardField.AUTHOR, false),
                        new SaveOrder.SortCriterion(StandardField.YEAR, true),
                        new SaveOrder.SortCriterion(StandardField.ABSTRACT, false))),
                saveOrderConfig.get());
    }

    @Test
    void integrationTestCustomKeyPattern() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{jabref-meta: keypattern_article:articleTest;}" + OS.NEWLINE
                        + "@comment{jabref-meta: keypatterndefault:test;}"));

        GlobalCitationKeyPatterns pattern = mock(GlobalCitationKeyPatterns.class);
        AbstractCitationKeyPatterns bibtexKeyPatterns = result.getMetaData().getCiteKeyPatterns(pattern);
        AbstractCitationKeyPatterns expectedPatterns = new DatabaseCitationKeyPatterns(pattern);
        expectedPatterns.setDefaultValue("test");
        expectedPatterns.addCitationKeyPattern(StandardEntryType.Article, "articleTest");

        assertEquals(expectedPatterns, bibtexKeyPatterns);
    }

    @Test
    void integrationTestBiblatexMode() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{jabref-meta: databaseType:biblatex;}"));

        Optional<BibDatabaseMode> mode = result.getMetaData().getMode();

        assertEquals(BibDatabaseMode.BIBLATEX, mode.get());
    }

    @Test
    void integrationTestGroupTree() throws IOException {
        ParserResult result = parser.parse(new StringReader("""
                @comment{jabref-meta: groupsversion:3;}
                @comment{jabref-meta: groupstree:
                0 AllEntriesGroup:;
                1 KeywordGroup:Fréchet\\;0\\;keywords\\;FrechetSpace\\;0\\;1\\;;
                1 KeywordGroup:Invariant theory\\;0\\;keywords\\;GIT\\;0\\;0\\;;
                1 ExplicitGroup:TestGroup\\;0\\;Key1\\;Key2\\;;}"""));

        GroupTreeNode root = result.getMetaData().getGroups().get();

        assertEquals(new AllEntriesGroup("All entries"), root.getGroup());
        assertEquals(3, root.getNumberOfChildren());
        assertEquals(
                new RegexKeywordGroup("Fréchet", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "FrechetSpace", false),
                root.getChildren().getFirst().getGroup());
        assertEquals(
                new WordKeywordGroup("Invariant theory", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "GIT", false, ',', false),
                root.getChildren().get(1).getGroup());
        assertEquals(Arrays.asList("Key1", "Key2"),
                ((ExplicitGroup) root.getChildren().get(2).getGroup()).getLegacyEntryKeys());
    }

    /**
     * Checks that BibDesk Static Groups are available after parsing the library
     */
    @Test
    void integrationTestBibDeskStaticGroup() throws Exception {
        ParserResult result = parser.parse(new StringReader("""
                @article{Swain:2023aa,
                    author = {Subhashree Swain and P. Shalima and K.V.P. Latha},
                    date-added = {2023-09-14 20:09:08 +0200},
                    date-modified = {2023-09-14 20:09:08 +0200},
                    eprint = {2309.06758},
                    month = {09},
                    title = {Unravelling the Nuclear Dust Morphology of NGC 1365: A Two Phase Polar - RAT Model for the Ultraviolet to Infrared Spectral Energy Distribution},
                    url = {https://arxiv.org/pdf/2309.06758.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06758.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06758}}

                @article{Heyl:2023aa,
                    author = {Johannes Heyl and Joshua Butterworth and Serena Viti},
                    date-added = {2023-09-14 20:09:08 +0200},
                    date-modified = {2023-09-14 20:09:08 +0200},
                    eprint = {2309.06784},
                    month = {09},
                    title = {Understanding Molecular Abundances in Star-Forming Regions Using Interpretable Machine Learning},
                    url = {https://arxiv.org/pdf/2309.06784.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06784.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06784}}

                @comment{BibDesk Static Groups{
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <array>
                    <dict>
                        <key>group name</key>
                        <string>firstTestGroup</string>
                        <key>keys</key>
                        <string>Swain:2023aa,Heyl:2023aa</string>
                    </dict>
                    <dict>
                        <key>group name</key>
                        <string>secondTestGroup</string>
                        <key>keys</key>
                        <string>Swain:2023aa</string>
                    </dict>
                </array>
                </plist>
                }}
                """));

        GroupTreeNode root = result.getMetaData().getGroups().get();
        assertEquals(new AllEntriesGroup("All entries"), root.getGroup());
        assertEquals(Optional.of(BIB_DESK_ROOT_GROUP_NAME), root.getFirstChild().map(GroupTreeNode::getName));

        ExplicitGroup firstTestGroupExpected = new ExplicitGroup("firstTestGroup", GroupHierarchyType.INDEPENDENT, ',');
        firstTestGroupExpected.setExpanded(true);

        assertEquals(Optional.of(firstTestGroupExpected), root.getFirstChild().flatMap(TreeNode::getFirstChild).map(GroupTreeNode::getGroup));

        ExplicitGroup secondTestGroupExpected = new ExplicitGroup("secondTestGroup", GroupHierarchyType.INDEPENDENT, ',');
        secondTestGroupExpected.setExpanded(true);
        assertEquals(Optional.of(secondTestGroupExpected), root.getFirstChild().flatMap(TreeNode::getLastChild).map(GroupTreeNode::getGroup));

        BibDatabase db = result.getDatabase();

        assertEquals(List.of(root.getGroup(), firstTestGroupExpected), root.getContainingGroups(db.getEntries(), true).stream().map(GroupTreeNode::getGroup).toList());
        assertEquals(List.of(root.getGroup(), firstTestGroupExpected), root.getContainingGroups(db.getEntryByCitationKey("Heyl:2023aa").stream().toList(), false).stream().map(GroupTreeNode::getGroup).toList());
    }

    /**
     * Checks that BibDesk Smart Groups are available after parsing the library
     */
    @Test
    @Disabled("Not yet supported")
    void integrationTestBibDeskSmartGroup() throws Exception {
        ParserResult result = parser.parse(new StringReader("""
                @article{Kraljic:2023aa,
                    author = {Katarina Kraljic and Florent Renaud and Yohan Dubois and Christophe Pichon and Oscar Agertz and Eric Andersson and Julien Devriendt and Jonathan Freundlich and Sugata Kaviraj and Taysun Kimm and Garreth Martin and S{\\'e}bastien Peirani and {\\'A}lvaro Segovia Otero and Marta Volonteri and Sukyoung K. Yi},
                    date-added = {2023-09-14 20:09:10 +0200},
                    date-modified = {2023-09-14 20:09:10 +0200},
                    eprint = {2309.06485},
                    month = {09},
                    title = {Emergence and cosmic evolution of the Kennicutt-Schmidt relation driven by interstellar turbulence},
                    url = {https://arxiv.org/pdf/2309.06485.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06485.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06485}}

                @article{Swain:2023aa,
                    author = {Subhashree Swain and P. Shalima and K.V.P. Latha},
                    date-added = {2023-09-14 20:09:08 +0200},
                    date-modified = {2023-09-14 20:09:08 +0200},
                    eprint = {2309.06758},
                    month = {09},
                    title = {Unravelling the Nuclear Dust Morphology of NGC 1365: A Two Phase Polar - RAT Model for the Ultraviolet to Infrared Spectral Energy Distribution},
                    url = {https://arxiv.org/pdf/2309.06758.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06758.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06758}}

                @article{Heyl:2023aa,
                    author = {Johannes Heyl and Joshua Butterworth and Serena Viti},
                    date-added = {2023-09-14 20:09:08 +0200},
                    date-modified = {2023-09-14 20:09:08 +0200},
                    eprint = {2309.06784},
                    month = {09},
                    title = {Understanding Molecular Abundances in Star-Forming Regions Using Interpretable Machine Learning},
                    url = {https://arxiv.org/pdf/2309.06784.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06784.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06784}}

                @comment{BibDesk Smart Groups{
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <array>
                    <dict>
                        <key>conditions</key>
                        <array>
                            <dict>
                                <key>comparison</key>
                                <integer>4</integer>
                                <key>key</key>
                                <string>BibTeX Type</string>
                                <key>value</key>
                                <string>article</string>
                                <key>version</key>
                                <string>1</string>
                            </dict>
                            <dict>
                                <key>comparison</key>
                                <integer>2</integer>
                                <key>key</key>
                                <string>Title</string>
                                <key>value</key>
                                <string>the</string>
                                <key>version</key>
                                <string>1</string>
                            </dict>
                        </array>
                        <key>conjunction</key>
                        <integer>0</integer>
                        <key>group name</key>
                        <string>article</string>
                    </dict>
                    <dict>
                        <key>conditions</key>
                        <array>
                            <dict>
                                <key>comparison</key>
                                <integer>3</integer>
                                <key>key</key>
                                <string>Author</string>
                                <key>value</key>
                                <string>Swain</string>
                                <key>version</key>
                                <string>1</string>
                            </dict>
                        </array>
                        <key>conjunction</key>
                        <integer>0</integer>
                        <key>group name</key>
                        <string>Swain</string>
                    </dict>
                </array>
                </plist>
                }}
                """));

        GroupTreeNode root = result.getMetaData().getGroups().get();
        assertEquals(new AllEntriesGroup("All entries"), root.getGroup());
        assertEquals(2, root.getNumberOfChildren());
        ExplicitGroup firstTestGroupExpected = new ExplicitGroup("article", GroupHierarchyType.INDEPENDENT, ',');
        firstTestGroupExpected.setExpanded(false);
        assertEquals(firstTestGroupExpected, root.getChildren().getFirst().getGroup());
        ExplicitGroup secondTestGroupExpected = new ExplicitGroup("Swain", GroupHierarchyType.INDEPENDENT, ',');
        secondTestGroupExpected.setExpanded(false);
        assertEquals(secondTestGroupExpected, root.getChildren().get(1).getGroup());

        BibDatabase db = result.getDatabase();
        List<BibEntry> firstTestGroupEntriesExpected = new ArrayList<>();
        firstTestGroupEntriesExpected.add(db.getEntryByCitationKey("Kraljic:2023aa").get());
        firstTestGroupEntriesExpected.add(db.getEntryByCitationKey("Swain:2023aa").get());
        assertTrue(root.getChildren().getFirst().getGroup().containsAll(firstTestGroupEntriesExpected));
        assertFalse(root.getChildren().get(1).getGroup().contains(db.getEntryByCitationKey("Swain:2023aa").get()));
    }

    /**
     * Checks that both BibDesk Static Groups and Smart Groups are available after parsing the library
     */
    @Test
    @Disabled("Not yet supported")
    void integrationTestBibDeskMultipleGroup() throws Exception {
        ParserResult result = parser.parse(new StringReader("""
                @article{Kraljic:2023aa,
                    author = {Katarina Kraljic and Florent Renaud and Yohan Dubois and Christophe Pichon and Oscar Agertz and Eric Andersson and Julien Devriendt and Jonathan Freundlich and Sugata Kaviraj and Taysun Kimm and Garreth Martin and S{\\'e}bastien Peirani and {\\'A}lvaro Segovia Otero and Marta Volonteri and Sukyoung K. Yi},
                    date-added = {2023-09-14 20:09:10 +0200},
                    date-modified = {2023-09-14 20:09:10 +0200},
                    eprint = {2309.06485},
                    month = {09},
                    title = {Emergence and cosmic evolution of the Kennicutt-Schmidt relation driven by interstellar turbulence},
                    url = {https://arxiv.org/pdf/2309.06485.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06485.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06485}}

                @article{Swain:2023aa,
                    author = {Subhashree Swain and P. Shalima and K.V.P. Latha},
                    date-added = {2023-09-14 20:09:08 +0200},
                    date-modified = {2023-09-14 20:09:08 +0200},
                    eprint = {2309.06758},
                    month = {09},
                    title = {Unravelling the Nuclear Dust Morphology of NGC 1365: A Two Phase Polar - RAT Model for the Ultraviolet to Infrared Spectral Energy Distribution},
                    url = {https://arxiv.org/pdf/2309.06758.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06758.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06758}}

                @article{Heyl:2023aa,
                    author = {Johannes Heyl and Joshua Butterworth and Serena Viti},
                    date-added = {2023-09-14 20:09:08 +0200},
                    date-modified = {2023-09-14 20:09:08 +0200},
                    eprint = {2309.06784},
                    month = {09},
                    title = {Understanding Molecular Abundances in Star-Forming Regions Using Interpretable Machine Learning},
                    url = {https://arxiv.org/pdf/2309.06784.pdf},
                    year = {2023},
                    bdsk-url-1 = {https://arxiv.org/pdf/2309.06784.pdf},
                    bdsk-url-2 = {https://arxiv.org/abs/2309.06784}}

                @comment{BibDesk Static Groups{
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <array>
                    <dict>
                        <key>group name</key>
                        <string>firstTestGroup</string>
                        <key>keys</key>
                        <string>Swain:2023aa,Heyl:2023aa</string>
                    </dict>
                </array>
                </plist>
                }}

                @comment{BibDesk Smart Groups{
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <array>
                    <dict>
                        <key>conditions</key>
                        <array>
                            <dict>
                                <key>comparison</key>
                                <integer>4</integer>
                                <key>key</key>
                                <string>BibTeX Type</string>
                                <key>value</key>
                                <string>article</string>
                                <key>version</key>
                                <string>1</string>
                            </dict>
                            <dict>
                                <key>comparison</key>
                                <integer>2</integer>
                                <key>key</key>
                                <string>Title</string>
                                <key>value</key>
                                <string>the</string>
                                <key>version</key>
                                <string>1</string>
                            </dict>
                        </array>
                        <key>conjunction</key>
                        <integer>0</integer>
                        <key>group name</key>
                        <string>article</string>
                    </dict>
                </array>
                </plist>
                }}
                """));

        GroupTreeNode root = result.getMetaData().getGroups().get();
        assertEquals(new AllEntriesGroup("All entries"), root.getGroup());
        assertEquals(2, root.getNumberOfChildren());
        ExplicitGroup firstTestGroupExpected = new ExplicitGroup("firstTestGroup", GroupHierarchyType.INDEPENDENT, ',');
        firstTestGroupExpected.setExpanded(false);
        assertEquals(firstTestGroupExpected, root.getChildren().getFirst().getGroup());
        ExplicitGroup secondTestGroupExpected = new ExplicitGroup("article", GroupHierarchyType.INDEPENDENT, ',');
        secondTestGroupExpected.setExpanded(false);
        assertEquals(secondTestGroupExpected, root.getChildren().get(1).getGroup());

        BibDatabase db = result.getDatabase();
        assertTrue(root.getChildren().getFirst().getGroup().containsAll(db.getEntries()));
        List<BibEntry> smartGroupEntriesExpected = new ArrayList<>();
        smartGroupEntriesExpected.add(db.getEntryByCitationKey("Kraljic:2023aa").get());
        smartGroupEntriesExpected.add(db.getEntryByCitationKey("Swain:2023aa").get());
        assertTrue(root.getChildren().getFirst().getGroup().containsAll(smartGroupEntriesExpected));
    }

    /**
     * Checks that a TexGroup finally gets the required data, after parsing the library.
     */
    @Test
    void integrationTestTexGroup() throws Exception {
        ParserResult result = parser.parse(new StringReader(
                "@comment{jabref-meta: grouping:" + OS.NEWLINE
                        + "0 AllEntriesGroup:;" + OS.NEWLINE
                        + "1 TexGroup:cited entries\\;0\\;paper.aux\\;1\\;0x8a8a8aff\\;\\;\\;;"
                        + "}" + OS.NEWLINE
                        + "@Comment{jabref-meta: databaseType:biblatex;}" + OS.NEWLINE
                        + "@Comment{jabref-meta: fileDirectory:src/test/resources/org/jabref/model/groups;}" + OS.NEWLINE
                        + "@Comment{jabref-meta: fileDirectory-"
                        + System.getProperty("user.name") + "-"
                        + InetAddress.getLocalHost().getHostName()
                        + ":src/test/resources/org/jabref/model/groups;}" + OS.NEWLINE
                        + "@Comment{jabref-meta: fileDirectoryLatex-"
                        + System.getProperty("user.name") + "-"
                        + InetAddress.getLocalHost().getHostName()
                        + ":src/test/resources/org/jabref/model/groups;}" + OS.NEWLINE
        ));

        GroupTreeNode root = result.getMetaData().getGroups().get();

        assertEquals(((TexGroup) root.getChildren().getFirst().getGroup()).getFilePath(),
                Path.of("src/test/resources/org/jabref/model/groups/paper.aux"));
    }

    @Test
    void integrationTestProtectedFlag() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{jabref-meta: protectedFlag:true;}"));

        assertTrue(result.getMetaData().isProtected());
    }

    @Test
    void integrationTestContentSelectors() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@Comment{jabref-meta: selector_pubstate:approved;captured;received;status;}"));

        List<String> values = new ArrayList<>(4);
        values.add("approved");
        values.add("captured");
        values.add("received");
        values.add("status");

        assertEquals(values, result.getMetaData().getContentSelectors().getSelectorValuesForField(StandardField.PUBSTATE));
    }

    @Test
    void parseReallyUnknownType() throws Exception {
        String bibtexEntry = """
                @ReallyUnknownType{test,
                 Comment                  = {testentry}
                }""";

        Collection<BibEntry> entries = parser.parseEntries(bibtexEntry);
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType(new UnknownEntryType("Reallyunknowntype"));
        expectedEntry.setCitationKey("test");
        expectedEntry.setField(StandardField.COMMENT, "testentry");

        assertEquals(List.of(expectedEntry), entries);
    }

    @Test
    void parseOtherTypeTest() throws Exception {
        String bibtexEntry = """
                @Other{test,
                 Comment                  = {testentry}
                }""";

        Collection<BibEntry> entries = parser.parseEntries(bibtexEntry);
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType(new UnknownEntryType("Other"));
        expectedEntry.setCitationKey("test");
        expectedEntry.setField(StandardField.COMMENT, "testentry");

        assertEquals(List.of(expectedEntry), entries);
    }

    @Test
    void parseRecognizesDatabaseID() throws Exception {
        String expectedDatabaseID = "q1w2e3r4t5z6";
        String sharedDatabaseFileContent = "\\% DBID: " + expectedDatabaseID +
                OS.NEWLINE +
                "@Article{a}";

        ParserResult parserResult = parser.parse(new StringReader(sharedDatabaseFileContent));
        String actualDatabaseID = parserResult.getDatabase().getSharedDatabaseID().get();

        assertEquals(expectedDatabaseID, actualDatabaseID);
    }

    @Test
    void parseDoesNotRecognizeDatabaseIDasUserComment() throws Exception {
        String sharedDatabaseFileContent = "\\% Encoding: UTF-8" + OS.NEWLINE +
                "\\% DBID: q1w2e3r4t5z6" + OS.NEWLINE +
                "@Article{a}";

        ParserResult parserResult = parser.parse(new StringReader(sharedDatabaseFileContent));
        List<BibEntry> entries = parserResult.getDatabase().getEntries();

        assertEquals(1, entries.size());
        assertEquals("", entries.getFirst().getUserComments());
    }

    @Test
    void integrationTestFileDirectories() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}"
                        + "@comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"
                        + "@comment{jabref-meta: fileDirectoryLatex-defaultOwner-user:D:\\\\Latex;}"));

        assertEquals("\\Literature\\", result.getMetaData().getDefaultFileDirectory().get());
        assertEquals("D:\\Documents", result.getMetaData().getUserFileDirectory("defaultOwner-user").get());
        assertEquals("D:\\Latex", result.getMetaData().getLatexFileDirectory("defaultOwner-user").get().toString());
    }

    @ParameterizedTest
    @CsvSource({
            // single backslash kept
            "C:\\temp\\test",
            "\\\\servername\\path\\to\\file",
            "//servername/path/to/file",
            "."})
    void fileDirectoriesUnmodified(String directory) throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@comment{jabref-meta: fileDirectory:" + directory + "}"));
        assertEquals(directory, result.getMetaData().getDefaultFileDirectory().get());
    }

    @ParameterizedTest
    @CsvSource({
            "C:\\temp\\test, C:\\\\temp\\\\test",
            "\\\\servername\\path\\to\\file, \\\\\\\\servername\\\\path\\\\to\\\\file"})
    void fileDirectoryWithDoubleEscapeIsRead(String expected, String provided) throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@comment{jabref-meta: fileDirectory: " + provided + "}"));
        assertEquals(expected, result.getMetaData().getDefaultFileDirectory().get());
    }

    @Test
    void parseReturnsEntriesInSameOrder() throws IOException {
        List<BibEntry> expected = new ArrayList<>();
        BibEntry first = new BibEntry();
        first.setType(StandardEntryType.Article);
        first.setCitationKey("a");
        expected.add(first);

        BibEntry second = new BibEntry();
        second.setType(StandardEntryType.Article);
        second.setCitationKey("b");
        expected.add(second);

        BibEntry third = new BibEntry();
        third.setType(StandardEntryType.InProceedings);
        third.setCitationKey("c");
        expected.add(third);

        ParserResult result = parser
                .parse(new StringReader("""
                        @article{a}
                        @article{b}
                        @inProceedings{c}"""));

        assertEquals(expected, result.getDatabase().getEntries());
    }

    @Test
    void parsePrecedingComment() throws IOException {
        String bibtexEntry = """
                % Some random comment that should stay here
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }""";

        // read in bibtex string
        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(1, entries.size());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(5, entry.getFields().size());
        assertTrue(entry.getFields().contains(StandardField.AUTHOR));
        assertEquals(Optional.of("Foo Bar"), entry.getField(StandardField.AUTHOR));
        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void parseCommentAndEntryInOneLine() throws IOException {
        String bibtexEntry = """
                Some random comment that should stay here @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }""";

        // read in bibtex string
        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(1, entries.size());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(5, entry.getFields().size());
        assertTrue(entry.getFields().contains(StandardField.AUTHOR));
        assertEquals(Optional.of("Foo Bar"), entry.getField(StandardField.AUTHOR));
        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void preserveEncodingPrefixInsideEntry() throws ParseException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, SaveConfiguration.ENCODING_PREFIX);

        List<BibEntry> parsed = parser
                .parseEntries("@article{test,author={" + SaveConfiguration.ENCODING_PREFIX + "}}");

        assertEquals(List.of(expected), parsed);
    }

    @Test
    void parseBracketedComment() throws IOException {
        String commentText = "@Comment{someComment}";

        ParserResult result = parser.parse(new StringReader(commentText));

        assertEquals(commentText, result.getDatabase().getEpilog());
    }

    @Test
    void parseRegularCommentBeforeEntry() throws IOException {
        String bibtexEntry = """
                @Comment{someComment}
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }""";

        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void parseCommentWithoutBrackets() throws IOException {
        String commentText = "@Comment someComment";

        ParserResult result = parser.parse(new StringReader(commentText));

        assertEquals(commentText, result.getDatabase().getEpilog());
    }

    @Test
    void parseCommentWithoutBracketsBeforeEntry() throws IOException {
        String bibtexEntry = """
                @Comment someComment
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }""";

        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void parseCommentContainingEntries() throws IOException {
        String bibtexEntry = """
                @Comment{@article{myarticle,}
                @inproceedings{blabla, title={the proceedings of blabla}; }
                }
                @Article{test,
                  Author                   = {Foo Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }""";

        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void parseCommentContainingEntriesAndAtSymbols() throws IOException {
        String bibtexEntry = """
                @Comment{@article{myarticle,}
                @inproceedings{blabla, title={the proceedings of bl@bl@}; }
                }
                @Article{test,
                  Author                   = {Foo@Bar},
                  Journal                  = {International Journal of Something},
                  Note                     = {some note},
                  Number                   = {1}
                }""";

        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void parseEmptyPreambleLeadsToEmpty() throws IOException {
        ParserResult result = parser.parse(new StringReader("@preamble{}"));

        assertFalse(result.hasWarnings());
        assertEquals(Optional.empty(), result.getDatabase().getPreamble());
    }

    @Test
    void parseEmptyFileLeadsToPreamble() throws IOException {
        ParserResult result = parser.parse(new StringReader(""));

        assertFalse(result.hasWarnings());
        assertEquals(Optional.empty(), result.getDatabase().getPreamble());
    }

    @Test
    void parseYearWithMonthString() throws Exception {
        Optional<BibEntry> result = parser.parseSingleEntry("@ARTICLE{HipKro03, year = {2003}, month = feb }");

        assertEquals(new Date(2003, 2), result.get().getPublicationDate().get());
    }

    @Test
    void parseYearWithIllFormattedMonthString() throws Exception {
        Optional<BibEntry> result = parser.parseSingleEntry("@ARTICLE{HipKro03, year = {2003}, month = #FEB# }");

        assertEquals(new Date(2003, 2), result.get().getPublicationDate().get());
    }

    @Test
    void parseYearWithMonthNumber() throws Exception {
        Optional<BibEntry> result = parser.parseSingleEntry("@ARTICLE{HipKro03, year = {2003}, month = 2 }");

        assertEquals(new Date(2003, 2), result.get().getPublicationDate().get());
    }

    @Test
    void parseYear() throws Exception {
        Optional<BibEntry> result = parser.parseSingleEntry("@ARTICLE{HipKro03, year = {2003} }");

        assertEquals(new Date(2003), result.get().getPublicationDate().get());
    }

    @Test
    void parseEntryUsingStringConstantsForTwoAuthorsWithEtAsStringConstant() throws ParseException {
        // source of the example: https://docs.jabref.org/fields/strings
        Collection<BibEntry> parsed = parser
                .parseEntries("@String { kopp = \"Kopp, Oliver\" }" +
                        "@String { kubovy = \"Kubovy, Jan\" }" +
                        "@String { et = \" and \" }" +
                        "@Misc{m1, author = kopp # et # kubovy }");

        BibEntry expectedEntry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("m1")
                .withField(StandardField.AUTHOR, "#kopp##et##kubovy#");

        assertEquals(List.of(expectedEntry), parsed);
    }

    @Test
    void parseStringConstantsForTwoAuthorsHasCorrectBibTeXEntry() throws ParseException {
        // source of the example: https://docs.jabref.org/fields/strings
        Collection<BibEntry> parsed = parser
                .parseEntries("@String { kopp = \"Kopp, Oliver\" }" +
                        "@String { kubovy = \"Kubovy, Jan\" }" +
                        "@String { et = \" and \" }" +
                        "@Misc{m2, author = kopp # \" and \" # kubovy }");

        BibEntry expectedEntry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("m2")
                .withField(StandardField.AUTHOR, "#kopp# and #kubovy#");

        assertEquals(List.of(expectedEntry), parsed);
    }

    @Test
    void parseStringConstantsForTwoAuthors() throws ParseException {
        // source of the example: https://docs.jabref.org/fields/strings
        Collection<BibEntry> parsed = parser
                .parseEntries("@String { kopp = \"Kopp, Oliver\" }" +
                        "@String { kubovy = \"Kubovy, Jan\" }" +
                        "@String { et = \" and \" }" +
                        "@Misc{m2, author = kopp # \" and \" # kubovy }");

        assertEquals("#kopp# and #kubovy#", parsed.iterator().next().getField(StandardField.AUTHOR).get());
    }

    @Test
    void textAprilIsParsedAsMonthApril() throws ParseException {
        Optional<BibEntry> result = parser.parseSingleEntry("@Misc{m, month = \"apr\" }");

        assertEquals(Month.APRIL, result.get().getMonth().get());
    }

    @Test
    void textAprilIsDisplayedAsConstant() throws ParseException {
        Optional<BibEntry> result = parser.parseSingleEntry("@Misc{m, month = \"apr\" }");

        assertEquals("apr", result.get().getField(StandardField.MONTH).get());
    }

    @Test
    void bibTeXConstantAprilIsParsedAsMonthApril() throws ParseException {
        Optional<BibEntry> result = parser.parseSingleEntry("@Misc{m, month = apr }");

        assertEquals(Month.APRIL, result.get().getMonth().get());
    }

    @Test
    void bibTeXConstantAprilIsDisplayedAsConstant() throws ParseException {
        Optional<BibEntry> result = parser.parseSingleEntry("@Misc{m, month = apr }");

        assertEquals("#apr#", result.get().getField(StandardField.MONTH).get());
    }

    @Test
    void bibTeXConstantAprilIsParsedAsStringMonthAprilWhenReadingTheField() throws ParseException {
        Optional<BibEntry> result = parser.parseSingleEntry("@Misc{m, month = apr }");

        assertEquals(Optional.of("#apr#"), result.get().getField(StandardField.MONTH));
    }

    @Test
    void parseDuplicateKeywordsWithOnlyOneEntry() throws ParseException {
        Optional<BibEntry> result = parser.parseSingleEntry("""
                @Article{,
                Keywords={asdf,asdf,asdf},
                }
                """);

        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.KEYWORDS, "asdf,asdf,asdf");

        assertEquals(Optional.of(expectedEntry), result);
    }

    @Test
    void parseDuplicateKeywordsWithTwoEntries() throws Exception {
        BibEntry expectedEntryFirst = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.KEYWORDS, "bbb")
                .withCitationKey("Test2017");

        BibEntry expectedEntrySecond = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.KEYWORDS, "asdf,asdf,asdf");

        String entries = """
                @Article{Test2017,
                  keywords = {bbb},
                }

                @Article{,
                  keywords = {asdf,asdf,asdf},
                },
                """;
        ParserResult result = parser.parse(new StringReader(entries));
        assertEquals(List.of(expectedEntryFirst, expectedEntrySecond), result.getDatabase().getEntries());
    }

    @Test
    void parseBibDeskLinkedFiles() throws IOException {

        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article);
        expectedEntry.withCitationKey("Kovakkuni:2023aa")
                     .withField(StandardField.AUTHOR, "Navyasree Kovakkuni and Federico Lelli and Pierre-alain Duc and M{\\'e}d{\\'e}ric Boquien and Jonathan Braine and Elias Brinks and Vassilis Charmandaris and Francoise Combes and Jeremy Fensch and Ute Lisenfeld and Stacy McGaugh and J. Chris Mihos and Marcel. S. Pawlowski and Yves. Revaz and Peter. M. Weilbacher")
                     .withField(new UnknownField("date-added"), "2023-09-14 20:09:12 +0200")
                     .withField(new UnknownField("date-modified"), "2023-09-14 20:09:12 +0200")
                     .withField(StandardField.EPRINT, "2309.06478")
                     .withField(StandardField.MONTH, "09")
                     .withField(StandardField.TITLE, "Molecular and Ionized Gas in Tidal Dwarf Galaxies: The Spatially Resolved Star-Formation Relation")
                     .withField(StandardField.URL, "https://arxiv.org/pdf/2309.06478.pdf")
                     .withField(StandardField.YEAR, "2023")
                     .withField(new UnknownField("bdsk-url-1"), "https://arxiv.org/abs/2309.06478")
                     .withField(StandardField.FILE, ":../../Downloads/2309.06478.pdf:");

        ParserResult result = parser.parse(new StringReader("""
                @article{Kovakkuni:2023aa,
                    author = {Navyasree Kovakkuni and Federico Lelli and Pierre-alain Duc and M{\\'e}d{\\'e}ric Boquien and Jonathan Braine and Elias Brinks and Vassilis Charmandaris and Francoise Combes and Jeremy Fensch and Ute Lisenfeld and Stacy McGaugh and J. Chris Mihos and Marcel. S. Pawlowski and Yves. Revaz and Peter. M. Weilbacher},
                    date-added = {2023-09-14 20:09:12 +0200},
                    date-modified = {2023-09-14 20:09:12 +0200},
                    eprint = {2309.06478},
                    month = {09},
                    title = {Molecular and Ionized Gas in Tidal Dwarf Galaxies: The Spatially Resolved Star-Formation Relation},
                    url = {https://arxiv.org/pdf/2309.06478.pdf},
                    year = {2023},
                    bdsk-file-1 = {YnBsaXN0MDDSAQIDBFxyZWxhdGl2ZVBhdGhZYWxpYXNEYXRhXxAeLi4vLi4vRG93bmxvYWRzLzIzMDkuMDY0NzgucGRmTxEBUgAAAAABUgACAAAMTWFjaW50b3NoIEhEAAAAAAAAAAAAAAAAAAAA4O/yLkJEAAH/////DjIzMDkuMDY0NzgucGRmAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP/////hKRkeAAAAAAAAAAAAAgACAAAKIGN1AAAAAAAAAAAAAAAAAAlEb3dubG9hZHMAAAIAKy86VXNlcnM6Y2hyaXN0b3BoczpEb3dubG9hZHM6MjMwOS4wNjQ3OC5wZGYAAA4AHgAOADIAMwAwADkALgAwADYANAA3ADgALgBwAGQAZgAPABoADABNAGEAYwBpAG4AdABvAHMAaAAgAEgARAASAClVc2Vycy9jaHJpc3RvcGhzL0Rvd25sb2Fkcy8yMzA5LjA2NDc4LnBkZgAAEwABLwAAFQACABH//wAAAAgADQAaACQARQAAAAAAAAIBAAAAAAAAAAUAAAAAAAAAAAAAAAAAAAGb},
                    bdsk-url-1 = {https://arxiv.org/abs/2309.06478}}
                    }
                """));
        BibDatabase database = result.getDatabase();

        assertEquals(Collections.singletonList(expectedEntry), database.getEntries());
    }

    @Test
    void parseInvalidBibDeskFilesResultsInWarnings() throws IOException {
        // the first entry is invalid base 64, the second entry is correct plist format but does not contain the key
        String entries = """
                @Article{Test2017,
                    bdsk-file-1 = {////=},
                }

                @Article{Test2,
                   bdsk-file-1 = {YnBsaXN0MDDUAQIDBAUGJCVYJHZlcnNpb25YJG9iamVjdHNZJGFyY2hpdmVyVCR0b3ASAAGGoKgHCBMUFRYaIVUkbnVsbNMJCgsMDxJXTlMua2V5c1pOUy5vYmplY3RzViRjbGFzc6INDoACgAOiEBGABIAFgAdccmVsYXRpdmVQYXRoWWFsaWFzRGF0YV8QVi4uLy4uLy4uL1BhcGVycy9Bc2hlaW0yMDA1IFRoZSBHZW9ncmFwaHkgb2YgSW5ub3ZhdGlvbiBSZWdpb25hbCBJbm5vdmF0aW9uIFN5c3RlbXMucGRm0hcLGBlXTlMuZGF0YU8RAkoAAAAAAkoAAgAADE1hY2ludG9zaCBIRAAAAAAAAAAAAAAAAAAAAM6T/wtIKwAAACI+9B9Bc2hlaW0yMDA1IFRoZSBHZW9nciMyMjQ4QzkucGRmAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIkjJw6jvRAAAAAAAAAAAAAMAAgAACSAAAAAAAAAAAAAAAAAAAAAGUGFwZXJzABAACAAAzpPw+wAAABEACAAAw6jhNAAAAAEAEAAiPvQAIjTXACHV2wAHw2AAAgBQTWFjaW50b3NoIEhEOlVzZXJzOgBpbGlwcGVydDoARG9jdW1lbnRzOgBQYXBlcnM6AEFzaGVpbTIwMDUgVGhlIEdlb2dyIzIyNDhDOS5wZGYADgCOAEYAQQBzAGgAZQBpAG0AMgAwADAANQAgAFQAaABlACAARwBlAG8AZwByAGEAcABoAHkAIABvAGYAIABJAG4AbgBvAHYAYQB0AGkAbwBuACAAUgBlAGcAaQBvAG4AYQBsACAASQBuAG4AbwB2AGEAdABpAG8AbgAgAFMAeQBzAHQAZQBtAHMALgBwAGQAZgAPABoADABNAGEAYwBpAG4AdABvAHMAaAAgAEgARAASAGZVc2Vycy9pbGlwcGVydC9Eb2N1bWVudHMvUGFwZXJzL0FzaGVpbTIwMDUgVGhlIEdlb2dyYXBoeSBvZiBJbm5vdmF0aW9uIFJlZ2lvbmFsIElubm92YXRpb24gU3lzdGVtcy5wZGYAEwABLwAAFQACAA///wAAgAbSGxwdHlokY2xhc3NuYW1lWCRjbGFzc2VzXU5TTXV0YWJsZURhdGGjHR8gVk5TRGF0YVhOU09iamVjdNIbHCIjXE5TRGljdGlvbmFyeaIiIF8QD05TS2V5ZWRBcmNoaXZlctEmJ1Ryb290gAEACAARABoAIwAtADIANwBAAEYATQBVAGAAZwBqAGwAbgBxAHMAdQB3AIQAjgDnAOwA9ANCA0QDSQNUA10DawNvA3YDfwOEA5EDlAOmA6kDrgAAAAAAAAIBAAAAAAAAACgAAAAAAAAAAAAAAAAAAAOw},
                },
                """;
        ParserResult result = parser.parse(new StringReader(entries));

        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Test2017");
        BibEntry secondEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Test2");

        assertEquals(List.of(firstEntry, secondEntry), result.getDatabase().getEntries());
    }
}
