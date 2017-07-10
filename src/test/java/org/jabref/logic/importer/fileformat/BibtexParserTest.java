package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.OS;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.DatabaseBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.SaveOrderConfig;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the BibtexParser
 */
public class BibtexParserTest {

    private ImportFormatPreferences importFormatPreferences;
    private BibtexParser parser;


    @Before
    public void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        parser = new BibtexParser(importFormatPreferences);
    }

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void parseWithNullThrowsNullPointerException() throws Exception {
        parser.parse(null);
    }

    @Test
    public void fromStringRecognizesEntry() throws ParseException {
        List<BibEntry> parsed = parser
                .parseEntries("@article{test,author={Ed von Test}}");

        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setCiteKey("test");
        expected.setField("author", "Ed von Test");
        assertEquals(Collections.singletonList(expected), parsed);
    }

    @Test
    public void fromStringReturnsEmptyListFromEmptyString() throws ParseException {
        Collection<BibEntry> parsed = parser.parseEntries("");
        assertNotNull(parsed);
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    public void fromStringReturnsEmptyListIfNoEntryRecognized() throws ParseException {
        Collection<BibEntry> parsed = parser
                .parseEntries("@@article@@{{{{{{}");
        assertNotNull(parsed);
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    public void singleFromStringRecognizesEntry() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString(
                "@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n",
                importFormatPreferences);

        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setCiteKey("canh05");
        expected.setField("author", "Crowston, K. and Annabi, H.");
        expected.setField("title", "Title A");
        assertEquals(Optional.of(expected), parsed);
    }

    @Test
    public void singleFromStringRecognizesEntryInMultiple() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString(
                "@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n"
                        + "@inProceedings{foo," + "  author={Norton Bar}}",
                importFormatPreferences);

        assertTrue(parsed.get().getCiteKeyOptional().equals(Optional.of("canh05"))
                || parsed.get().getCiteKeyOptional().equals(Optional.of("foo")));
    }

    @Test
    public void singleFromStringReturnsEmptyFromEmptyString() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString("", importFormatPreferences);
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    public void singleFromStringReturnsEmptyIfNoEntryRecognized() throws ParseException {
        Optional<BibEntry> parsed = BibtexParser.singleFromString("@@article@@{{{{{{}", importFormatPreferences);
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    public void parseRecognizesEntry() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseQuotedEntries() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Ed von Test\"}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryOnlyWithKey() throws IOException {

        ParserResult result = parser.parse(new StringReader("@article{test}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
    }

    @Test
    public void parseRecognizesEntryWithWhitespaceAtBegining() throws IOException {

        ParserResult result = parser
                .parse(new StringReader(" @article{test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithWhitespace() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article { test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithNewlines() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article\n{\ntest,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithUnknownType() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@unknown{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("unknown", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithVeryLongType() throws IOException {

        ParserResult result = parser.parse(
                new StringReader("@thisIsALongStringToTestMaybeItIsToLongWhoKnowsNOTme{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("thisisalongstringtotestmaybeitistolongwhoknowsnotme", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryInParenthesis() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article(test,author={Ed von Test})"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    /**
     * Test for [ 1594123 ] Failure to import big numbers Issue Reported by Ulf Martin. SF Bugs #503, #495 are also
     * related
     */
    @Test
    public void parseRecognizesEntryWithBigNumbers() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05," + "isbn = 1234567890123456789,\n"
                + "isbn2 = {1234567890123456789},\n" + "small = 1234,\n" + "}"), importFormatPreferences);

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();

        assertEquals("article", e.getType());
        assertEquals(Optional.of("canh05"), e.getCiteKeyOptional());
        assertEquals(Optional.of("1234567890123456789"), e.getField("isbn"));
        assertEquals(Optional.of("1234567890123456789"), e.getField("isbn2"));
        assertEquals(Optional.of("1234"), e.getField("small"));
    }

    @Test
    public void parseRecognizesBibtexKeyWithSpecialCharacters() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{te_st:with-special(characters),author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();

        assertEquals("article", e.getType());
        assertEquals(Optional.of("te_st:with-special(characters)"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWhereLastFieldIsFinishedWithComma() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test},}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithAtInField() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von T@st}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();

        BibEntry expected = new BibEntry("article").withField(BibEntry.KEY_FIELD, "test")
                .withField("author", "Ed von T@st");

        assertEquals(Collections.singletonList(expected), parsed);
    }

    @Test
    public void parseRecognizesEntryPrecedingComment() throws IOException {
        String comment = "@Comment{@article{myarticle,}" + OS.NEWLINE
                + "@inproceedings{blabla, title={the proceedings of bl@bl@}; }" + OS.NEWLINE + "}";
        String entryWithComment = comment + OS.NEWLINE + "@article{test,author={Ed von T@st}}";
        ParserResult result = parser.parse(new StringReader(entryWithComment));

        List<BibEntry> parsed = result.getDatabase().getEntries();

        BibEntry expected = new BibEntry("article")
                .withField(BibEntry.KEY_FIELD, "test")
                .withField("author", "Ed von T@st");
        expected.setCommentsBeforeEntry(comment);

        assertEquals(Collections.singletonList(expected), parsed);

        assertEquals(expected.getUserComments(), parsed.get(0).getUserComments());
    }

    @Test
    public void parseRecognizesMultipleEntries() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n" + "@inProceedings{foo," + "  author={Norton Bar}}"),
                importFormatPreferences);
        List<BibEntry> parsed = result.getDatabase().getEntries();

        List<BibEntry> expected = new ArrayList<>();
        BibEntry firstEntry = new BibEntry();
        firstEntry.setType("article");
        firstEntry.setCiteKey("canh05");
        firstEntry.setField("author", "Crowston, K. and Annabi, H.");
        firstEntry.setField("title", "Title A");
        expected.add(firstEntry);

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType("inproceedings");
        secondEntry.setCiteKey("foo");
        secondEntry.setField("author", "Norton Bar");
        expected.add(secondEntry);

        assertEquals(expected, parsed);
    }

    @Test
    public void parseSetsParsedSerialization() throws IOException {
        String firstEntry = "@article{canh05," + "  author = {Crowston, K. and Annabi, H.}," + OS.NEWLINE
                + "  title = {Title A}}" + OS.NEWLINE;
        String secondEntry = "@inProceedings{foo," + "  author={Norton Bar}}";

        ParserResult result = parser
                .parse(new StringReader(firstEntry + secondEntry));

        for (BibEntry entry : result.getDatabase().getEntries()) {
            if (entry.getCiteKeyOptional().get().equals("canh05")) {
                assertEquals(firstEntry, entry.getParsedSerialization());
            } else {
                assertEquals(secondEntry, entry.getParsedSerialization());
            }
        }
    }

    @Test
    public void parseRecognizesMultipleEntriesOnSameLine() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{canh05}" + "@inProceedings{foo}"));
        List<BibEntry> parsed = result.getDatabase().getEntries();

        List<BibEntry> expected = new ArrayList<>();
        BibEntry firstEntry = new BibEntry();
        firstEntry.setType("article");
        firstEntry.setCiteKey("canh05");
        expected.add(firstEntry);

        BibEntry secondEntry = new BibEntry();
        secondEntry.setType("inproceedings");
        secondEntry.setCiteKey("foo");
        expected.add(secondEntry);

        assertEquals(expected, parsed);
    }

    @Test
    public void parseCombinesMultipleAuthorFields() throws IOException {

        ParserResult result = parser.parse(
                new StringReader("@article{test,author={Ed von Test},author={Second Author},author={Third Author}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test and Second Author and Third Author"), e.getField("author"));
    }

    @Test
    public void parseCombinesMultipleEditorFields() throws IOException {

        ParserResult result = parser.parse(
                new StringReader("@article{test,editor={Ed von Test},editor={Second Author},editor={Third Author}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test and Second Author and Third Author"), e.getField("editor"));
    }

    /**
     * Test for SF Bug #1269
     */
    @Test
    public void parseCombinesMultipleKeywordsFields() throws IOException {

        ParserResult result = parser.parse(
                new StringReader("@article{test,Keywords={Test},Keywords={Second Keyword},Keywords={Third Keyword}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Test, Second Keyword, Third Keyword"), e.getField("keywords"));
    }

    @Test
    public void parseRecognizesHeaderButIgnoresEncoding() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("This file was created with JabRef 2.1 beta 2." + "\n"
                + "Encoding: Cp1252" + "\n" + "" + "\n" + "@INPROCEEDINGS{CroAnnHow05," + "\n"
                + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n"
                + "  title = {Effective work practices for floss development: A model and propositions}," + "\n"
                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n"
                + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n" + "  timestamp = {2006.05.29}," + "\n"
                + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"),
                importFormatPreferences);
        assertEquals(Optional.empty(), result.getMetaData().getEncoding());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("inproceedings", e.getType());
        assertEquals(8, e.getFieldNames().size());
        assertEquals(Optional.of("CroAnnHow05"), e.getCiteKeyOptional());
        assertEquals(Optional.of("Crowston, K. and Annabi, H. and Howison, J. and Masango, C."), e.getField("author"));
        assertEquals(Optional.of("Effective work practices for floss development: A model and propositions"),
                e.getField("title"));
        assertEquals(Optional.of("Hawaii International Conference On System Sciences (HICSS)"),
                e.getField("booktitle"));
        assertEquals(Optional.of("2005"), e.getField("year"));
        assertEquals(Optional.of("oezbek"), e.getField("owner"));
        assertEquals(Optional.of("2006.05.29"), e.getField("timestamp"));
        assertEquals(Optional.of("http://james.howison.name/publications.html"), e.getField("url"));
    }

    @Test
    public void parseRecognizesFormatedEntry() throws IOException {
        ParserResult result = BibtexParser.parse(
                new StringReader("" + "@INPROCEEDINGS{CroAnnHow05," + "\n"
                        + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n"
                        + "  title = {Effective work practices for floss development: A model and propositions}," + "\n"
                        + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n"
                        + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n" + "  timestamp = {2006.05.29},"
                        + "\n" + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"),
                importFormatPreferences);
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("inproceedings", e.getType());
        assertEquals(8, e.getFieldNames().size());
        assertEquals(Optional.of("CroAnnHow05"), e.getCiteKeyOptional());
        assertEquals(Optional.of("Crowston, K. and Annabi, H. and Howison, J. and Masango, C."), e.getField("author"));
        assertEquals(Optional.of("Effective work practices for floss development: A model and propositions"),
                e.getField("title"));
        assertEquals(Optional.of("Hawaii International Conference On System Sciences (HICSS)"),
                e.getField("booktitle"));
        assertEquals(Optional.of("2005"), e.getField("year"));
        assertEquals(Optional.of("oezbek"), e.getField("owner"));
        assertEquals(Optional.of("2006.05.29"), e.getField("timestamp"));
        assertEquals(Optional.of("http://james.howison.name/publications.html"), e.getField("url"));
    }

    @Test
    public void parseRecognizesFieldValuesInQuotationMarks() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Ed von Test\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseRecognizesNumbersWithoutBracketsOrQuotationMarks() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,year = 2005}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("2005"), e.getField("year"));
    }

    @Test
    public void parseRecognizesUppercaseFields() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,AUTHOR={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    /**
     * Test for SF Bug #806
     */
    @Test
    public void parseRecognizesAbsoluteFile() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,file = {D:\\Documents\\literature\\Tansel-PRL2006.pdf}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("D:\\Documents\\literature\\Tansel-PRL2006.pdf"), e.getField("file"));
    }

    /**
     * Test for SF Bug #48
     */
    @Test
    public void parseRecognizesDateFieldWithConcatenation() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,date = {1-4~} # nov}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("1-4~#nov#"), e.getField("date"));
    }

    @Test
    public void parseReturnsEmptyListIfNoEntryRecognized() throws IOException {
        ParserResult result = BibtexParser.parse(
                new StringReader("  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.}," + "\n"
                        + "  title = {Effective work practices for floss development: A model and propositions}," + "\n"
                        + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)}," + "\n"
                        + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n" + "  timestamp = {2006.05.29},"
                        + "\n" + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"),
                importFormatPreferences);
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseReturnsEmptyListIfNoEntryExistent() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("This was created with JabRef 2.1 beta 2." + "\n" + "Encoding: Cp1252" + "\n"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseRecognizesDuplicateBibtexKeys() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n" + "@inProceedings{canh05," + "  author={Norton Bar}}"),
                        importFormatPreferences);

        List<String> duplicateKeys = result.getDuplicateKeys();
        assertEquals(1, duplicateKeys.size());
        assertEquals("canh05", duplicateKeys.get(0));
    }

    @Test
    public void parseNotWarnsAboutEntryWithoutBibtexKey() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{,author={Ed von Test}}"));

        assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();

        BibEntry e = new BibEntry();
        e.setField("author", "Ed von Test");
        e.setType("article");

        assertEquals(Collections.singletonList(e), c);
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracket() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={author missing bracket}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    /**
     * Test for SF bug 482
     */
    @Test
    public void parseAddsEscapedOpenBracketToFieldValue() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,review={escaped \\{ bracket}}"));

        assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(Optional.of("escaped \\{ bracket"), e.getField("review"));
    }

    @Test
    public void parseAddsEscapedClosingBracketToFieldValue() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,review={escaped \\} bracket}}"));

        assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(Optional.of("escaped \\} bracket"), e.getField("review"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracketInQuotationMarks() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"author {missing bracket\"}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseIgnoresArbitraryContentAfterEntry() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={author bracket }}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals("Size should be one, but was " + c.size(), 1, c.size());
        assertEquals("Epilog should be preserved", "}", result.getDatabase().getEpilog());
    }

    @Test
    public void parseWarnsAboutUnmatchedContentInEntryWithoutComma() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author bracket } too much}"),
                importFormatPreferences);

        List<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals(Optional.of("author bracket #too##much#"), entries.get(0).getField("author"));
    }

    @Test
    public void parseWarnsAboutUnmatchedContentInEntry() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author bracket }, too much}"),
                importFormatPreferences);

        assertTrue("There should be warnings", result.hasWarnings());

        List<BibEntry> entries = result.getDatabase().getEntries();
        assertEquals("Size should be zero, but was " + entries.size(), 0, entries.size());
    }

    @Test
    public void parseAcceptsEntryWithAtSymbolInBrackets() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={author @ good}}"));

        List<BibEntry> entries = result.getDatabase().getEntries();

        assertEquals(1, entries.size());
        assertEquals(Optional.of("author @ good"), entries.get(0).getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithAtSymbolInQuotationMarks() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"author @ good\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("author @ good"), e.getField("author"));
    }

    @Test
    public void parseRecognizesFieldsWithBracketsEnclosedInQuotationMarks() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Test {Ed {von} Test}\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Test {Ed {von} Test}"), e.getField("author"));
    }

    @Test
    public void parseRecognizesFieldsWithEscapedQuotationMarks() throws IOException {

        // Quotes in fields of the form key = "value" have to be escaped by putting them into braces
        ParserResult result = parser
                .parse(new StringReader("@article{test,author=\"Test {\" Test}\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Test {\" Test}"), e.getField("author"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithFieldsThatAreNotSeperatedByComma() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test} year=2005}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseIgnoresAndWarnsAboutCorruptedEntryButRecognizeOthers() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader(
                        "@article{test,author={author missing bracket}" + "@article{test,author={Ed von Test}}"),
                importFormatPreferences);

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    /**
     * Test for SF Bug #1283
     */
    @Test
    public void parseRecognizesMonthFieldsWithFollowingComma() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test},month={8,}},"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(3, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
        assertEquals(Optional.of("8,"), e.getField("month"));
    }

    @Test
    public void parseRecognizesPreamble() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@preamble{some text and \\latex}"));
        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesUppercasePreamble() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@PREAMBLE{some text and \\latex}"));
        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleWithWhitespace() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@preamble {some text and \\latex}"));
        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleInParenthesis() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@preamble(some text and \\latex)"));
        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleWithConcatenation() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@preamble{\"some text\" # \"and \\latex\"}"));
        assertEquals(Optional.of("\"some text\" # \"and \\latex\""), result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesString() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}"));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseSavesOneNewlineAfterStringInParsedSerialization() throws IOException {

        String string = "@string{bourdieu = {Bourdieu, Pierre}}" + OS.NEWLINE;
        ParserResult result = parser
                .parse(new StringReader(string + OS.NEWLINE + OS.NEWLINE));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals(string, s.getParsedSerialization());
    }

    @Test
    public void parseRecognizesStringWithWhitespace() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@string {bourdieu = {Bourdieu, Pierre}}"));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseRecognizesStringInParenthesis() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@string(bourdieu = {Bourdieu, Pierre})"));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseRecognizesMultipleStrings() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}" + "@string{adieu = {Adieu, Pierre}}"));

        assertEquals(2, result.getDatabase().getStringCount());
        Iterator<BibtexString> iterator = result.getDatabase().getStringValues().iterator();
        BibtexString s = iterator.next();
        BibtexString t = iterator.next();

        // Sort them because we can't be sure about the order
        if (s.getName().equals("adieu")) {
            BibtexString tmp = s;
            s = t;
            t = tmp;
        }

        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());
        assertEquals("adieu", t.getName());
        assertEquals("Adieu, Pierre", t.getContent());
    }

    @Test
    public void parseRecognizesStringAndEntry() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("" + "@string{bourdieu = {Bourdieu, Pierre}}"
                        + "@book{bourdieu-2002-questions-sociologie, " + "	Address = {Paris}," + "	Author = bourdieu,"
                        + "	Isbn = 2707318256," + "	Publisher = {Minuit}," + "	Title = {Questions de sociologie},"
                        + "	Year = 2002" + "}"),
                importFormatPreferences);

        assertEquals(1, result.getDatabase().getStringCount());
        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();

        assertEquals("book", e.getType());
        assertEquals(Optional.of("bourdieu-2002-questions-sociologie"), e.getCiteKeyOptional());
        assertEquals(Optional.of("Paris"), e.getField("address"));
        assertEquals(Optional.of("#bourdieu#"), e.getField("author"));
        assertEquals(Optional.of("2707318256"), e.getField("isbn"));
        assertEquals(Optional.of("Minuit"), e.getField("publisher"));
        assertEquals(Optional.of("Questions de sociologie"), e.getField("title"));
        assertEquals(Optional.of("2002"), e.getField("year"));
    }

    @Test
    public void parseWarnsAboutStringsWithSameNameAndOnlyKeepsOne() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}" + "@string{bourdieu = {Other}}"));
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getDatabase().getStringCount());
    }

    @Test
    public void parseIgnoresComments() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@comment{some text and \\latex}"));
        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresUpercaseComments() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@COMMENT{some text and \\latex}"));
        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresCommentsBeforeEntry() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@comment{some text and \\latex}" + "@article{test,author={Ed von Test}}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseIgnoresCommentsAfterEntry() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "@comment{some text and \\latex}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseIgnoresText() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("comment{some text and \\latex"));
        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresTextBeforeEntry() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("comment{some text and \\latex" + "@article{test,author={Ed von Test}}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseIgnoresTextAfterEntry() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "comment{some text and \\latex"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(2, e.getFieldNames().size());
        assertEquals(Optional.of("Ed von Test"), e.getField("author"));
    }

    @Test
    public void parseConvertsNewlineToSpace() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\nb}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals(Optional.of("a b"), e.getField("a"));
    }

    @Test
    public void parseConvertsMultipleNewlinesToSpace() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\n\nb}," + "b = {a\n \nb}," + "c = {a \n \n b}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals(Optional.of("a b"), e.getField("a"));
        assertEquals(Optional.of("a b"), e.getField("b"));
        assertEquals(Optional.of("a b"), e.getField("c"));
    }

    @Test
    public void parseConvertsTabToSpace() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\tb}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals(Optional.of("a b"), e.getField("a"));
    }

    @Test
    public void parseConvertsMultipleTabsToSpace() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,a = {a\t\tb}," + "b = {a\t \tb}," + "c = {a \t \t b}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals(Optional.of("a b"), e.getField("a"));
        assertEquals(Optional.of("a b"), e.getField("b"));
        assertEquals(Optional.of("a b"), e.getField("c"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    public void parsePreservesMultipleSpacesInNonWrappableField() throws IOException {
        when(importFormatPreferences.getFieldContentParserPreferences().getNonWrappableFields())
                .thenReturn(Collections.singletonList("file"));
        ParserResult result = parser
                .parse(new StringReader("@article{canh05,file = {ups  sala}}"), importFormatPreferences);

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals(Optional.of("ups  sala"), e.getField("file"));
    }

    @Test
    public void parsePreservesTabsInAbstractField() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,abstract = {ups  \tsala}}"),
                importFormatPreferences);
        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals(Optional.of("ups  \tsala"), e.getField(FieldName.ABSTRACT));
    }

    @Test
    public void parsePreservesNewlineInAbstractField() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,abstract = {ups \nsala}}"),
                importFormatPreferences);

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals(Optional.of("ups " + OS.NEWLINE + "sala"), e.getField(FieldName.ABSTRACT));
    }

    /**
     * Test for #650
     */
    @Test
    public void parseHandlesAccentsCorrectly() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@article{test,author = {H\'{e}lne Fiaux}}"));
        assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(Optional.of("H\'{e}lne Fiaux"), e.getField("author"));
    }

    /**
     * Test for #669
     */
    @Test
    public void parsePreambleAndEntryWithoutNewLine() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("@preamble{some text and \\latex}@article{test,author = {H\'{e}lne Fiaux}}"));
        assertFalse(result.hasWarnings());

        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals(Optional.of("test"), e.getCiteKeyOptional());
        assertEquals(Optional.of("H\'{e}lne Fiaux"), e.getField("author"));
    }

    /**
     * Test for #669
     */
    @Test
    public void parseFileHeaderAndPreambleWithoutNewLine() throws IOException {

        ParserResult result = parser
                .parse(new StringReader("% Encoding: US-ASCII@preamble{some text and \\latex}"));
        assertFalse(result.hasWarnings());

        assertEquals(Optional.of("some text and \\latex"), result.getDatabase().getPreamble());
    }

    @Test
    public void parseSavesEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser.parse(new StringReader(testEntry));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry, e.getParsedSerialization());
    }

    @Test
    public void parseSavesOneNewlineAfterEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntry + OS.NEWLINE + OS.NEWLINE));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry + OS.NEWLINE, e.getParsedSerialization());
    }

    @Test
    public void parseSavesNewlinesBeforeEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntry));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntry, e.getParsedSerialization());
    }

    @Test
    public void parseRemovesEncodingLineInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser.parse(
                new StringReader(SavePreferences.ENCODING_PREFIX + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntry));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(OS.NEWLINE + OS.NEWLINE + testEntry, e.getParsedSerialization());
    }

    @Test
    public void parseSavesNewlinesBetweenEntriesInParsedSerialization() throws IOException {
        String testEntryOne = "@article{test1,author={Ed von Test}}";
        String testEntryTwo = "@article{test2,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntryOne + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + testEntryTwo));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(2, c.size());

        Iterator<BibEntry> i = c.iterator();
        BibEntry a = i.next();
        BibEntry b = i.next();

        // Sort them because we can't be sure about the order
        if (a.getCiteKeyOptional().equals(Optional.of("test2"))) {
            BibEntry tmp = a;
            a = b;
            b = tmp;
        }

        assertEquals(testEntryOne + OS.NEWLINE, a.getParsedSerialization());
        assertEquals(OS.NEWLINE + OS.NEWLINE + testEntryTwo, b.getParsedSerialization());
    }

    @Test
    public void parseIgnoresWhitespaceInEpilogue() throws IOException {
        ParserResult result = parser.parse(new StringReader("   " + OS.NEWLINE));

        assertEquals("", result.getDatabase().getEpilog());
    }

    @Test
    public void parseIgnoresWhitespaceInEpilogueAfterEntry() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntry + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + "  " + OS.NEWLINE));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry + OS.NEWLINE, e.getParsedSerialization());
        assertEquals("", result.getDatabase().getEpilog());
    }

    @Test
    public void parseTrimsWhitespaceInEpilogueAfterEntry() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = parser
                .parse(new StringReader(testEntry + OS.NEWLINE + OS.NEWLINE + OS.NEWLINE + " epilogue " + OS.NEWLINE));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry + OS.NEWLINE, e.getParsedSerialization());
        assertEquals("epilogue", result.getDatabase().getEpilog());
    }

    @Test
    public void parseRecognizesSaveActionsAfterEntry() throws IOException {
        BibtexParser parser = this.parser;

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
        assertEquals(Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())),
                saveActions.getConfiguredActions());
    }

    @Test
    public void integrationTestSaveActions() throws IOException {
        BibtexParser parser = this.parser;

        ParserResult parserResult = parser
                .parse(new StringReader("@comment{jabref-meta: saveActions:enabled;title[lower_case]}"));
        FieldFormatterCleanups saveActions = parserResult.getMetaData().getSaveActions().get();

        assertTrue(saveActions.isEnabled());
        assertEquals(Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())),
                saveActions.getConfiguredActions());
    }

    @Test
    public void integrationTestCustomEntryType() throws IOException {
        ParserResult result = parser.parse(
                new StringReader("@comment{jabref-entrytype: Lecturenotes: req[author;title] opt[language;url]}"));

        Map<String, EntryType> customEntryTypes = result.getEntryTypes();

        assertEquals(1, customEntryTypes.size());
        assertEquals("Lecturenotes", customEntryTypes.keySet().toArray()[0]);
        EntryType entryType = customEntryTypes.get("Lecturenotes");
        assertEquals("Lecturenotes", entryType.getName());
        assertEquals(Arrays.asList("author", "title"), entryType.getRequiredFields());
        assertEquals(Arrays.asList("language", "url"), entryType.getOptionalFields());
    }

    @Test
    public void integrationTestSaveOrderConfig() throws IOException {
        ParserResult result = BibtexParser.parse(
                new StringReader(
                        "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"),
                importFormatPreferences);

        Optional<SaveOrderConfig> saveOrderConfig = result.getMetaData().getSaveOrderConfig();

        assertEquals(new SaveOrderConfig(false, new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true), new SaveOrderConfig.SortCriterion("abstract", false)),
                saveOrderConfig.get());
    }

    @Test
    public void integrationTestCustomKeyPattern() throws IOException {
        ParserResult result = BibtexParser
                .parse(new StringReader("@comment{jabref-meta: keypattern_article:articleTest;}" + OS.NEWLINE
                        + "@comment{jabref-meta: keypatterndefault:test;}"), importFormatPreferences);

        GlobalBibtexKeyPattern pattern = mock(GlobalBibtexKeyPattern.class);
        AbstractBibtexKeyPattern bibtexKeyPattern = result.getMetaData().getCiteKeyPattern(pattern);

        AbstractBibtexKeyPattern expectedPattern = new DatabaseBibtexKeyPattern(pattern);
        expectedPattern.setDefaultValue("test");
        expectedPattern.addBibtexKeyPattern("article", "articleTest");

        assertEquals(expectedPattern, bibtexKeyPattern);
    }

    @Test
    public void integrationTestBiblatexMode() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{jabref-meta: databaseType:biblatex;}"));

        Optional<BibDatabaseMode> mode = result.getMetaData().getMode();

        assertEquals(BibDatabaseMode.BIBLATEX, mode.get());
    }

    @Test
    public void integrationTestGroupTree() throws IOException, ParseException {
        ParserResult result = BibtexParser.parse(new StringReader("@comment{jabref-meta: groupsversion:3;}" + OS.NEWLINE
                + "@comment{jabref-meta: groupstree:" + OS.NEWLINE + "0 AllEntriesGroup:;" + OS.NEWLINE
                + "1 KeywordGroup:Fréchet\\;0\\;keywords\\;FrechetSpace\\;0\\;1\\;;" + OS.NEWLINE
                + "1 KeywordGroup:Invariant theory\\;0\\;keywords\\;GIT\\;0\\;0\\;;" + OS.NEWLINE
                + "1 ExplicitGroup:TestGroup\\;0\\;Key1\\;Key2\\;;" + "}"), importFormatPreferences);

        GroupTreeNode root = result.getMetaData().getGroups().get();

        assertEquals(new AllEntriesGroup("All entries"), root.getGroup());
        assertEquals(3, root.getNumberOfChildren());
        assertEquals(
                new RegexKeywordGroup("Fréchet", GroupHierarchyType.INDEPENDENT, "keywords", "FrechetSpace", false),
                root.getChildren().get(0).getGroup());
        assertEquals(
                new WordKeywordGroup("Invariant theory", GroupHierarchyType.INDEPENDENT, "keywords", "GIT", false, ',', false),
                root.getChildren().get(1).getGroup());
        assertEquals(Arrays.asList("Key1", "Key2"),
                ((ExplicitGroup) root.getChildren().get(2).getGroup()).getLegacyEntryKeys());
    }

    @Test
    public void integrationTestProtectedFlag() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@comment{jabref-meta: protectedFlag:true;}"));

        assertTrue(result.getMetaData().isProtected());
    }

    @Test
    public void integrationTestContentSelectors() throws IOException {
        ParserResult result = BibtexParser.parse(
                new StringReader("@Comment{jabref-meta: selector_status:approved;captured;received;status;}"), importFormatPreferences);

        List<String> values = new ArrayList(4);
        values.add("approved");
        values.add("captured");
        values.add("received");
        values.add("status");

        assertEquals(values, result.getMetaData().getContentSelectors().getSelectorValuesForField("status"));
    }

    @Test
    public void parseReallyUnknownType() throws Exception {
        String bibtexEntry = "@ReallyUnknownType{test," + OS.NEWLINE +
                " Comment                  = {testentry}" + OS.NEWLINE +
                "}";

        Collection<BibEntry> entries = parser.parseEntries(bibtexEntry);

        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType("Reallyunknowntype");
        expectedEntry.setCiteKey("test");
        expectedEntry.setField("comment", "testentry");

        assertEquals(Collections.singletonList(expectedEntry), entries);
    }

    @Test
    public void parseOtherTypeTest() throws Exception {
        String bibtexEntry = "@Other{test," + OS.NEWLINE +
                " Comment                  = {testentry}" + OS.NEWLINE +
                "}";

        Collection<BibEntry> entries = parser.parseEntries(bibtexEntry);

        BibEntry expectedEntry = new BibEntry();
        expectedEntry.setType("Other");
        expectedEntry.setCiteKey("test");
        expectedEntry.setField("comment", "testentry");

        assertEquals(Collections.singletonList(expectedEntry), entries);
    }


    @Test
    public void parseRecognizesDatabaseID() throws Exception {
        BibtexParser parser = this.parser;

        String expectedDatabaseID = "q1w2e3r4t5z6";

        StringBuilder sharedDatabaseFileContent = new StringBuilder()
                .append("% DBID: ").append(expectedDatabaseID)
                .append(OS.NEWLINE)
                .append("@Article{a}");

        ParserResult parserResult = parser.parse(new StringReader(sharedDatabaseFileContent.toString()));

        String actualDatabaseID = parserResult.getDatabase().getSharedDatabaseID().get();

        assertEquals(expectedDatabaseID, actualDatabaseID);
    }

    @Test
    public void parseDoesNotRecognizeDatabaseIDasUserComment() throws Exception {
        BibtexParser parser = this.parser;
        StringBuilder sharedDatabaseFileContent = new StringBuilder()
                .append("% Encoding: UTF-8").append(OS.NEWLINE)
                .append("% DBID: q1w2e3r4t5z6").append(OS.NEWLINE)
                .append("@Article{a}");

        ParserResult parserResult = parser.parse(new StringReader(sharedDatabaseFileContent.toString()));
        List<BibEntry> entries = parserResult.getDatabase().getEntries();

        assertEquals(1, entries.size());
        assertEquals("", entries.get(0).getUserComments());
    }

    @Test
    public void integrationTestFileDirectories() throws IOException {
        ParserResult result = BibtexParser.parse(
                new StringReader("@comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}"
                        + "@comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"),
                importFormatPreferences);

        assertEquals("\\Literature\\", result.getMetaData().getDefaultFileDirectory().get());
        assertEquals("D:\\Documents", result.getMetaData().getUserFileDirectory("defaultOwner-user").get());
    }

    @Test
    public void parseReturnsEntriesInSameOrder() throws IOException {
        ParserResult result = parser
                .parse(new StringReader("@article{a}" + OS.NEWLINE + "@article{b}" + OS.NEWLINE + "@inProceedings{c}"));

        List<BibEntry> expected = new ArrayList<>();
        BibEntry a = new BibEntry();
        a.setType("article");
        a.setCiteKey("a");
        expected.add(a);

        BibEntry b = new BibEntry();
        b.setType("article");
        b.setCiteKey("b");
        expected.add(b);

        BibEntry c = new BibEntry();
        c.setType("inproceedings");
        c.setCiteKey("c");
        expected.add(c);

        assertEquals(expected, result.getDatabase().getEntries());
    }

    @Test
    public void parsePrecedingComment() throws IOException {
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
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals(Optional.of("test"), entry.getCiteKeyOptional());
        assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals(Optional.of("Foo Bar"), entry.getField("author"));
        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    public void parseCommentAndEntryInOneLine() throws IOException {
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
        assertEquals(1, entries.size());

        BibEntry entry = entries.iterator().next();
        assertEquals(Optional.of("test"), entry.getCiteKeyOptional());
        assertEquals(5, entry.getFieldNames().size());
        Set<String> fields = entry.getFieldNames();
        assertTrue(fields.contains("author"));
        assertEquals(Optional.of("Foo Bar"), entry.getField("author"));
        assertEquals(bibtexEntry, entry.getParsedSerialization());
    }

    @Test
    public void preserveEncodingPrefixInsideEntry() throws ParseException {
        List<BibEntry> parsed = parser
                .parseEntries("@article{test,author={" + SavePreferences.ENCODING_PREFIX + "}}");

        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setCiteKey("test");
        expected.setField("author", SavePreferences.ENCODING_PREFIX);
        assertEquals(Collections.singletonList(expected), parsed);
    }

    @Test
    public void parseBracketedComment() throws IOException {
        String commentText = "@Comment{someComment}";
        ParserResult result = parser.parse(new StringReader(commentText));

        assertEquals(commentText, result.getDatabase().getEpilog());
    }

    @Test
    public void parseRegularCommentBeforeEntry() throws IOException {
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
    public void parseCommentWithoutBrackets() throws IOException {
        String commentText = "@Comment someComment";
        ParserResult result = parser.parse(new StringReader(commentText));

        assertEquals(commentText, result.getDatabase().getEpilog());
    }

    @Test
    public void parseCommentWithoutBracketsBeforeEntry() throws IOException {
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
    public void parseCommentContainingEntries() throws IOException {
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
    public void parseCommentContainingEntriesAndAtSymbols() throws IOException {
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
    public void parseEmptyPreambleLeadsToEmpty() throws IOException {
        ParserResult result = parser.parse(new StringReader("@preamble{}"));
        assertFalse(result.hasWarnings());
        assertEquals(Optional.empty(), result.getDatabase().getPreamble());
    }

    @Test
    public void parseEmptyFileLeadsToPreamble() throws IOException {
        ParserResult result = parser.parse(new StringReader(""));
        assertFalse(result.hasWarnings());
        assertEquals(Optional.empty(), result.getDatabase().getPreamble());
    }
}
