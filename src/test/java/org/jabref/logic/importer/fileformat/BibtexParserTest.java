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

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPattern;
import org.jabref.logic.citationkeypattern.DatabaseCitationKeyPattern;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.BibField;
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
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BibtexParserTest {

    private ImportFormatPreferences importFormatPreferences;
    private BibtexParser parser;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    @Test
    void parseWithNullThrowsNullPointerException() throws Exception {
        Executable toBeTested = () -> parser.parse(null);

        assertThrows(NullPointerException.class, toBeTested);
    }

    @Test
    void fromStringRecognizesEntry() throws ParseException {
        List<BibEntry> parsed = parser
                .parseEntries("@article{test,author={Ed von Test}}");

        BibEntry expected = new BibEntry();
        expected.setType(StandardEntryType.Article);
        expected.setCitationKey("test");
        expected.setField(StandardField.AUTHOR, "Ed von Test");

        assertEquals(Collections.singletonList(expected), parsed);
    }

    @Test
    void fromStringReturnsEmptyListFromEmptyString() throws ParseException {
        Collection<BibEntry> parsed = parser.parseEntries("");

        assertNotNull(parsed);
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    void fromStringReturnsEmptyListIfNoEntryRecognized() throws ParseException {
        Collection<BibEntry> parsed = parser
                .parseEntries("@@article@@{{{{{{}");

        assertNotNull(parsed);
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    void singleFromStringRecognizesEntry() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString(
                "@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n",
                importFormatPreferences, fileMonitor);

        BibEntry expected = new BibEntry();
        expected.setType(StandardEntryType.Article);
        expected.setCitationKey("canh05");
        expected.setField(StandardField.AUTHOR, "Crowston, K. and Annabi, H.");
        expected.setField(StandardField.TITLE, "Title A");

        assertEquals(Optional.of(expected), parsed);
    }

    @Test
    void singleFromStringRecognizesEntryInMultiple() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString(
                "@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n"
                        + "@inProceedings{foo," + "  author={Norton Bar}}",
                importFormatPreferences, fileMonitor);

        assertTrue(parsed.get().getCitationKey().equals(Optional.of("canh05"))
                || parsed.get().getCitationKey().equals(Optional.of("foo")));
    }

    @Test
    void singleFromStringReturnsEmptyFromEmptyString() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString("", importFormatPreferences, fileMonitor);

        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void singleFromStringReturnsEmptyIfNoEntryRecognized() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString("@@article@@{{{{{{}", importFormatPreferences, fileMonitor);

        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void parseRecognizesEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseQuotedEntries() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Ed von Test\"}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryOnlyWithKey() throws IOException {
        ParserResult result = parser.parse(new StringReader("@article{test}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
    }

    @Test
    void parseRecognizesEntryWithWhitespaceAtBegining() throws IOException {
        ParserResult result = parser
                .parse(new StringReader(" @article{test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithWhitespace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article { test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithNewlines() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article\n{\ntest,author={Ed von Test}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithUnknownType() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@unknown{test,author={Ed von Test}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(new UnknownEntryType("unknown"), entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithVeryLongType() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@thisIsALongStringToTestMaybeItIsToLongWhoKnowsNOTme{test,author={Ed von Test}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(new UnknownEntryType("thisisalongstringtotestmaybeitistolongwhoknowsnotme"), entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryInParenthesis() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article(test,author={Ed von Test})"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithBigNumbers() throws IOException {
        ParserResult result = parser.parse(new StringReader("@article{canh05," + "isbn = 1234567890123456789,\n"
                + "isbn2 = {1234567890123456789},\n" + "small = 1234,\n" + "}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("canh05"), entry.getCitationKey());
        assertEquals(Optional.of("1234567890123456789"), entry.getField(StandardField.ISBN));
        assertEquals(Optional.of("1234567890123456789"), entry.getField(new UnknownField("isbn2")));
        assertEquals(Optional.of("1234"), entry.getField(new UnknownField("small")));
    }

    @Test
    void parseRecognizesCitationKeyWithSpecialCharacters() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{te_st:with-special(characters),author={Ed von Test}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("te_st:with-special(characters)"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWhereLastFieldIsFinishedWithComma() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test},}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithAtInField() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von T@st}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.Article).withField(InternalField.KEY_FIELD, "test")
                                                                   .withField(StandardField.AUTHOR, "Ed von T@st");

        assertEquals(Collections.singletonList(expected), parsed);
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

        assertEquals(Collections.singletonList(expected), parsed);
        assertEquals(expected.getUserComments(), parsed.get(0).getUserComments());
    }

    @Test
    void parseRecognizesMultipleEntries() throws IOException {
        List<BibEntry> expected = new ArrayList<>();
        BibEntry firstEntry = new BibEntry();
        firstEntry.setType(StandardEntryType.Article);
        firstEntry.setCitationKey("canh05");
        firstEntry.setField(StandardField.AUTHOR, "Crowston, K. and Annabi, H.");
        firstEntry.setField(StandardField.TITLE, "Title A");
        expected.add(firstEntry);

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType(StandardEntryType.InProceedings);
        secondEntry.setCitationKey("foo");
        secondEntry.setField(StandardField.AUTHOR, "Norton Bar");
        expected.add(secondEntry);

        ParserResult result = parser.parse(
                new StringReader("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n" + "@inProceedings{foo," + "  author={Norton Bar}}"));
        List<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(expected, parsed);
    }

    @Test
    void parseSetsParsedSerialization() throws IOException {
        String firstEntry = "@article{canh05," + "  author = {Crowston, K. and Annabi, H.}," + OS.NEWLINE
                + "  title = {Title A}}" + OS.NEWLINE;
        String secondEntry = "@inProceedings{foo," + "  author={Norton Bar}}";

        ParserResult result = parser
                .parse(new StringReader(firstEntry + secondEntry));

        for (BibEntry entry : result.getDatabase().getEntries()) {
            if (entry.getCitationKey().equals(Optional.of("canh05"))) {
                assertEquals(firstEntry, entry.getParsedSerialization());
            } else {
                assertEquals(secondEntry, entry.getParsedSerialization());
            }
        }
    }

    @Test
    void parseRecognizesMultipleEntriesOnSameLine() throws IOException {
        List<BibEntry> expected = new ArrayList<>();
        BibEntry firstEntry = new BibEntry();
        firstEntry.setType(StandardEntryType.Article);
        firstEntry.setCitationKey("canh05");
        expected.add(firstEntry);

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType(StandardEntryType.InProceedings);
        secondEntry.setCitationKey("foo");
        expected.add(secondEntry);

        ParserResult result = parser
                .parse(new StringReader("@article{canh05}" + "@inProceedings{foo}"));
        List<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(expected, parsed);
    }

    @Test
    void parseCombinesMultipleAuthorFields() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@article{test,author={Ed von Test},author={Second Author},author={Third Author}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test and Second Author and Third Author"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseCombinesMultipleEditorFields() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@article{test,editor={Ed von Test},editor={Second Author},editor={Third Author}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test and Second Author and Third Author"), entry.getField(StandardField.EDITOR));
    }

    @Test
    void parseCombinesMultipleKeywordsFields() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@article{test,Keywords={Test},Keywords={Second Keyword},Keywords={Third Keyword}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Test, Second Keyword, Third Keyword"), entry.getField(StandardField.KEYWORDS));
    }

    @Test
    void parseRecognizesHeaderButIgnoresEncoding() throws IOException {
        ParserResult result = parser.parse(new StringReader("This file was created with JabRef 2.1 beta 2." + "\n"
                + "Encoding: Cp1252" + "\n" + "" + "\n" + "@INPROCEEDINGS{CroAnnHow05," + "\n"
                + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n"
                + "  title = {Effective work practices for floss development: A model and propositions}," + "\n"
                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n"
                + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n" + "  timestamp = {2006.05.29}," + "\n"
                + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"));

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
                new StringReader("" + "@INPROCEEDINGS{CroAnnHow05," + "\n"
                        + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n"
                        + "  title = {Effective work practices for floss development: A model and propositions}," + "\n"
                        + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n"
                        + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n" + "  timestamp = {2006.05.29},"
                        + "\n" + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

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
    void parseRecognizesFieldValuesInQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Ed von Test\"}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesNumbersWithoutBracketsOrQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,year = 2005}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("2005"), entry.getField(StandardField.YEAR));
    }

    @Test
    void parseRecognizesUppercaseFields() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,AUTHOR={Ed von Test}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesAbsoluteFile() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,file = {D:\\Documents\\literature\\Tansel-PRL2006.pdf}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("D:\\Documents\\literature\\Tansel-PRL2006.pdf"), entry.getField(StandardField.FILE));
    }

    @Test
    void parseRecognizesDateFieldWithConcatenation() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,date = {1-4~} # nov}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("1-4~#nov#"), entry.getField(StandardField.DATE));
    }

    @Test
    void parseReturnsEmptyListIfNoEntryRecognized() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n"
                        + "  title = {Effective work practices for floss development: A model and propositions}," + "\n"
                        + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n"
                        + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n" + "  timestamp = {2006.05.29},"
                        + "\n" + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(0, parsed.size());
    }

    @Test
    void parseReturnsEmptyListIfNoEntryExistent() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("This was created with JabRef 2.1 beta 2." + "\n" + "Encoding: Cp1252" + "\n"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(0, parsed.size());
    }

    @Test
    void parseNotWarnsAboutEntryWithoutCitationKey() throws IOException {
        BibEntry expected = new BibEntry();
        expected.setField(StandardField.AUTHOR, "Ed von Test");
        expected.setType(StandardEntryType.Article);

        ParserResult result = parser
                .parse(new StringReader("@article{,author={Ed von Test}}"));
        Collection<BibEntry> parsed = result.getDatabase().getEntries();

        assertFalse(result.hasWarnings());
        assertEquals(Collections.singletonList(expected), parsed);
    }

    @Test
    void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracket() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={author missing bracket}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(0, parsed.size());
        assertTrue(result.hasWarnings());
    }

    @Test
    void parseAddsEscapedOpenBracketToFieldValue() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,review={escaped \\{ bracket}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(Optional.of("escaped \\{ bracket"), entry.getField(StandardField.REVIEW));
        assertFalse(result.hasWarnings());
    }

    @Test
    void parseAddsEscapedClosingBracketToFieldValue() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,review={escaped \\} bracket}}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(Optional.of("escaped \\} bracket"), entry.getField(StandardField.REVIEW));
        assertFalse(result.hasWarnings());
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
    void parseIgnoresArbitraryContentAfterEntry() throws IOException {
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

        assertEquals(Optional.of("author bracket #too##much#"), entries.get(0).getField(StandardField.AUTHOR));
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
        assertEquals(Optional.of("author @ good"), entries.get(0).getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesEntryWithAtSymbolInQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"author @ good\"}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("author @ good"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesFieldsWithBracketsEnclosedInQuotationMarks() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Test {Ed {von} Test}\"}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Test {Ed {von} Test}"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseRecognizesFieldsWithEscapedQuotationMarks() throws IOException {
        // Quotes in fields of the form key = "value" have to be escaped by putting them into braces
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Test {\" Test}\"}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();
        BibEntry entry = parsed.iterator().next();

        assertEquals(1, parsed.size());
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("test"), entry.getCitationKey());
        assertEquals(2, entry.getFields().size());
        assertEquals(Optional.of("Test {\" Test}"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseIgnoresAndWarnsAboutEntryWithFieldsThatAreNotSeperatedByComma() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test} year=2005}"));

        Collection<BibEntry> parsed = result.getDatabase().getEntries();

        assertEquals(0, parsed.size());
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
        if (first.getName().equals("adieu")) {
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
                new StringReader("" + "@string{bourdieu = {Bourdieu, Pierre}}"
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

        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    void parseIgnoresUpercaseComments() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@COMMENT{some text and \\latex}"));

        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    void parseIgnoresCommentsBeforeEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{some text and \\latex}" + "@article{test,author={Ed von Test}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(StandardEntryType.Article, parsedEntry.getType());
        assertEquals(Optional.of("test"), parsedEntry.getCitationKey());
        assertEquals(2, parsedEntry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), parsedEntry.getField(StandardField.AUTHOR));
    }

    @Test
    void parseIgnoresCommentsAfterEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "@comment{some text and \\latex}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(1, parsedEntries.size());
        assertEquals(StandardEntryType.Article, parsedEntry.getType());
        assertEquals(Optional.of("test"), parsedEntry.getCitationKey());
        assertEquals(2, parsedEntry.getFields().size());
        assertEquals(Optional.of("Ed von Test"), parsedEntry.getField(StandardField.AUTHOR));
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
    void parseConvertsNewlineToSpace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\nb}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("a")));
    }

    @Test
    void parseConvertsMultipleNewlinesToSpace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\n\nb}," + "b = {a\n \nb}," + "c = {a \n \n b}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("a")));
        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("b")));
        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("c")));
    }

    @Test
    void parseConvertsTabToSpace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\tb}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("a")));
    }

    @Test
    void parseConvertsMultipleTabsToSpace() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\t\tb}," + "b = {a\t \tb}," + "c = {a \t \t b}}"));

        Collection<BibEntry> parsedEntries = result.getDatabase().getEntries();
        BibEntry parsedEntry = parsedEntries.iterator().next();

        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("a")));
        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("b")));
        assertEquals(Optional.of("a b"), parsedEntry.getField(new UnknownField("c")));
    }

    @Test
    void parsePreservesMultipleSpacesInNonWrappableField() throws IOException {
        when(importFormatPreferences.getFieldContentFormatterPreferences().getNonWrappableFields())
                .thenReturn(Collections.singletonList(StandardField.FILE));
        BibtexParser parser = new BibtexParser(importFormatPreferences, fileMonitor);
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

        assertEquals(Optional.of("ups " + OS.NEWLINE + "sala"), parsedEntry.getField(StandardField.ABSTRACT));
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
     * Test for #669
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
                new StringReader(SavePreferences.ENCODING_PREFIX + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntry));

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
                new StringReader("@InProceedings{6055279,\n" + "  Title                    = {Educational session 1},\n"
                        + "  Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},\n"
                        + "  Year                     = {2011},\n" + "  Month                    = {Sept},\n"
                        + "  Pages                    = {1-7},\n"
                        + "  Abstract                 = {Start of the above-titled section of the conference proceedings record.},\n"
                        + "  DOI                      = {10.1109/CICC.2011.6055279},\n"
                        + "  ISSN                     = {0886-5930}\n" + "}\n" + "\n"
                        + "@comment{jabref-meta: saveActions:enabled;title[lower_case]}"));

        FieldFormatterCleanups saveActions = parserResult.getMetaData().getSaveActions().get();

        assertTrue(saveActions.isEnabled());
        assertEquals(Collections.singletonList(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())),
                saveActions.getConfiguredActions());
    }

    @Test
    void parseRecognizesCRLFLineBreak() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@InProceedings{6055279,\r\n" + "  Title                    = {Educational session 1},\r\n"
                        + "  Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},\r\n"
                        + "  Year                     = {2011},\r\n" + "  Month                    = {Sept},\r\n"
                        + "  Pages                    = {1-7},\r\n"
                        + "  Abstract                 = {Start of the above-titled section of the conference proceedings record.},\r\n"
                        + "  DOI                      = {10.1109/CICC.2011.6055279},\r\n"
                        + "  ISSN                     = {0886-5930}\r\n" + "}\r\n"));
        assertEquals("\r\n", result.getDatabase().getNewLineSeparator());
    }

    @Test
    void parseRecognizesLFLineBreak() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@InProceedings{6055279,\n" + "  Title                    = {Educational session 1},\n"
                        + "  Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},\n"
                        + "  Year                     = {2011},\n" + "  Month                    = {Sept},\n"
                        + "  Pages                    = {1-7},\n"
                        + "  Abstract                 = {Start of the above-titled section of the conference proceedings record.},\n"
                        + "  DOI                      = {10.1109/CICC.2011.6055279},\n"
                        + "  ISSN                     = {0886-5930}\n" + "}\n"));
        assertEquals("\n", result.getDatabase().getNewLineSeparator());
    }

    @Test
    void integrationTestSaveActions() throws IOException {
        ParserResult parserResult = parser
                .parse(new StringReader("@comment{jabref-meta: saveActions:enabled;title[lower_case]}"));

        FieldFormatterCleanups saveActions = parserResult.getMetaData().getSaveActions().get();

        assertTrue(saveActions.isEnabled());
        assertEquals(Collections.singletonList(new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter())),
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

        Optional<SaveOrderConfig> saveOrderConfig = result.getMetaData().getSaveOrderConfig();

        assertEquals(new SaveOrderConfig(SaveOrderConfig.OrderType.SPECIFIED, List.of(
                new SaveOrderConfig.SortCriterion(StandardField.AUTHOR, false),
                new SaveOrderConfig.SortCriterion(StandardField.YEAR, true),
                new SaveOrderConfig.SortCriterion(StandardField.ABSTRACT, false))),
                saveOrderConfig.get());
    }

    @Test
    void integrationTestCustomKeyPattern() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{jabref-meta: keypattern_article:articleTest;}" + OS.NEWLINE
                        + "@comment{jabref-meta: keypatterndefault:test;}"));

        GlobalCitationKeyPattern pattern = mock(GlobalCitationKeyPattern.class);
        AbstractCitationKeyPattern bibtexKeyPattern = result.getMetaData().getCiteKeyPattern(pattern);
        AbstractCitationKeyPattern expectedPattern = new DatabaseCitationKeyPattern(pattern);
        expectedPattern.setDefaultValue("test");
        expectedPattern.addCitationKeyPattern(StandardEntryType.Article, "articleTest");

        assertEquals(expectedPattern, bibtexKeyPattern);
    }

    @Test
    void integrationTestBiblatexMode() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{jabref-meta: databaseType:biblatex;}"));

        Optional<BibDatabaseMode> mode = result.getMetaData().getMode();

        assertEquals(BibDatabaseMode.BIBLATEX, mode.get());
    }

    @Test
    void integrationTestGroupTree() throws IOException, ParseException {
        ParserResult result = parser.parse(new StringReader("@comment{jabref-meta: groupsversion:3;}" + OS.NEWLINE
                + "@comment{jabref-meta: groupstree:" + OS.NEWLINE + "0 AllEntriesGroup:;" + OS.NEWLINE
                + "1 KeywordGroup:Fréchet\\;0\\;keywords\\;FrechetSpace\\;0\\;1\\;;" + OS.NEWLINE
                + "1 KeywordGroup:Invariant theory\\;0\\;keywords\\;GIT\\;0\\;0\\;;" + OS.NEWLINE
                + "1 ExplicitGroup:TestGroup\\;0\\;Key1\\;Key2\\;;" + "}"));

        GroupTreeNode root = result.getMetaData().getGroups().get();

        assertEquals(new AllEntriesGroup("All entries"), root.getGroup());
        assertEquals(3, root.getNumberOfChildren());
        assertEquals(
                new RegexKeywordGroup("Fréchet", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "FrechetSpace", false),
                root.getChildren().get(0).getGroup());
        assertEquals(
                new WordKeywordGroup("Invariant theory", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "GIT", false, ',', false),
                root.getChildren().get(1).getGroup());
        assertEquals(Arrays.asList("Key1", "Key2"),
                ((ExplicitGroup) root.getChildren().get(2).getGroup()).getLegacyEntryKeys());
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

        assertEquals(((TexGroup) root.getChildren().get(0).getGroup()).getFilePath(),
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
        String bibtexEntry = "@ReallyUnknownType{test," + OS.NEWLINE +
                " Comment                  = {testentry}" + OS.NEWLINE +
                "}";

        Collection<BibEntry> entries = parser.parseEntries(bibtexEntry);
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType(new UnknownEntryType("Reallyunknowntype"));
        expectedEntry.setCitationKey("test");
        expectedEntry.setField(StandardField.COMMENT, "testentry");

        assertEquals(Collections.singletonList(expectedEntry), entries);
    }

    @Test
    void parseOtherTypeTest() throws Exception {
        String bibtexEntry = "@Other{test," + OS.NEWLINE +
                " Comment                  = {testentry}" + OS.NEWLINE +
                "}";

        Collection<BibEntry> entries = parser.parseEntries(bibtexEntry);
        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType(new UnknownEntryType("Other"));
        expectedEntry.setCitationKey("test");
        expectedEntry.setField(StandardField.COMMENT, "testentry");

        assertEquals(Collections.singletonList(expectedEntry), entries);
    }

    @Test
    void parseRecognizesDatabaseID() throws Exception {
        String expectedDatabaseID = "q1w2e3r4t5z6";
        StringBuilder sharedDatabaseFileContent = new StringBuilder()
                .append("\\% DBID: ").append(expectedDatabaseID)
                .append(OS.NEWLINE)
                .append("@Article{a}");

        ParserResult parserResult = parser.parse(new StringReader(sharedDatabaseFileContent.toString()));
        String actualDatabaseID = parserResult.getDatabase().getSharedDatabaseID().get();

        assertEquals(expectedDatabaseID, actualDatabaseID);
    }

    @Test
    void parseDoesNotRecognizeDatabaseIDasUserComment() throws Exception {
        StringBuilder sharedDatabaseFileContent = new StringBuilder()
                .append("\\% Encoding: UTF-8").append(OS.NEWLINE)
                .append("\\% DBID: q1w2e3r4t5z6").append(OS.NEWLINE)
                .append("@Article{a}");

        ParserResult parserResult = parser.parse(new StringReader(sharedDatabaseFileContent.toString()));
        List<BibEntry> entries = parserResult.getDatabase().getEntries();

        assertEquals(1, entries.size());
        assertEquals("", entries.get(0).getUserComments());
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
                .parse(new StringReader("@article{a}" + OS.NEWLINE + "@article{b}" + OS.NEWLINE + "@inProceedings{c}"));

        assertEquals(expected, result.getDatabase().getEntries());
    }

    @Test
    void parsePrecedingComment() throws IOException {
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
        // @formatter:off
        String bibtexEntry = "Some random comment that should stay here @Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

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
        BibEntry expected = new BibEntry();
        expected.setType(StandardEntryType.Article);
        expected.setCitationKey("test");
        expected.setField(StandardField.AUTHOR, SavePreferences.ENCODING_PREFIX);

        List<BibEntry> parsed = parser
                .parseEntries("@article{test,author={" + SavePreferences.ENCODING_PREFIX + "}}");

        assertEquals(Collections.singletonList(expected), parsed);
    }

    @Test
    void parseBracketedComment() throws IOException {
        String commentText = "@Comment{someComment}";

        ParserResult result = parser.parse(new StringReader(commentText));

        assertEquals(commentText, result.getDatabase().getEpilog());
    }

    @Test
    void parseRegularCommentBeforeEntry() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Comment{someComment} " + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

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
        // @formatter:off
        String bibtexEntry = "@Comment someComment  " + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void parseCommentContainingEntries() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Comment{@article{myarticle,}" + OS.NEWLINE +
                "@inproceedings{blabla, title={the proceedings of blabla}; }" + OS.NEWLINE +
                "} " + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

        ParserResult result = parser.parse(new StringReader(bibtexEntry));
        Collection<BibEntry> entries = result.getDatabase().getEntries();
        BibEntry entry = entries.iterator().next();

        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    void parseCommentContainingEntriesAndAtSymbols() throws IOException {
        // @formatter:off
        String bibtexEntry = "@Comment{@article{myarticle,}" + OS.NEWLINE +
                "@inproceedings{blabla, title={the proceedings of bl@bl@}; }" + OS.NEWLINE +
                "} " + OS.NEWLINE +
                "@Article{test," + OS.NEWLINE +
                "  Author                   = {Foo@Bar}," + OS.NEWLINE +
                "  Journal                  = {International Journal of Something}," + OS.NEWLINE +
                "  Note                     = {some note}," + OS.NEWLINE +
                "  Number                   = {1}" + OS.NEWLINE +
                "}";
        // @formatter:on

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
}
