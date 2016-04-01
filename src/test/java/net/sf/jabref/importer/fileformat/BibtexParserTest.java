package net.sf.jabref.importer.fileformat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.bibtex.BibtexEntryAssert;
import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.structure.AllEntriesGroup;
import net.sf.jabref.groups.structure.GroupHierarchyType;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import net.sf.jabref.logic.labelpattern.AbstractLabelPattern;
import net.sf.jabref.logic.labelpattern.DatabaseLabelPattern;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.EntryType;

/**
 * Test the BibtexParser
 */
public class BibtexParserTest {

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void initalizationWithNullThrowsNullPointerException() {
        new BibtexParser(null);
    }

    @Test
    public void fromStringRecognizesEntry() {
        List<BibEntry> parsed = BibtexParser.fromString("@article{test,author={Ed von Test}}");

        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setCiteKey("test");
        expected.setField("author", "Ed von Test");
        BibtexEntryAssert.assertEquals(Arrays.asList(expected), parsed);
    }

    @Test
    public void fromStringReturnsEmptyListFromEmptyString() {
        Collection<BibEntry> parsed = BibtexParser.fromString("");
        assertNotNull(parsed);
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    public void fromStringReturnsEmptyListIfNoEntryRecognized() {
        Collection<BibEntry> parsed = BibtexParser.fromString("@@article@@{{{{{{}");
        assertNotNull(parsed);
        assertEquals(Collections.emptyList(), parsed);
    }

    @Test
    public void singleFromStringRecognizesEntry() {
        BibEntry parsed = BibtexParser.singleFromString(
                "@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n");

        BibEntry expected = new BibEntry();
        expected.setType("article");
        expected.setCiteKey("canh05");
        expected.setField("author", "Crowston, K. and Annabi, H.");
        expected.setField("title", "Title A");
        BibtexEntryAssert.assertEquals(expected, parsed);
    }

    @Test
    public void singleFromStringRecognizesEntryInMultiple() {
        BibEntry parsed = BibtexParser
                .singleFromString("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n" + "@inProceedings{foo," + "  author={Norton Bar}}");

        assertTrue(parsed.getCiteKey().equals("canh05") || parsed.getCiteKey().equals("foo"));
    }

    @Test
    public void singleFromStringReturnsNullFromEmptyString() {
        BibEntry parsed = BibtexParser.singleFromString("");
        assertNull(parsed);
    }

    @Test
    public void singleFromStringReturnsNullIfNoEntryRecognized() {
        BibEntry parsed = BibtexParser.singleFromString("@@article@@{{{{{{}");
        assertNull(parsed);
    }

    @Test
    public void parseTwoTimesReturnsSameResult() throws IOException {
        BibtexParser parser = new BibtexParser(new StringReader("@article{test,author={Ed von Test}}"));
        ParserResult result = parser.parse();

        assertEquals(result, parser.parse());
    }

    @Test
    public void parseRecognizesEntry() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseQuotedEntries() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"Ed von Test\"}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryOnlyWithKey() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
    }

    @Test
    public void parseRecognizesEntryWithWhitespaceAtBegining() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(" @article{test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithWhitespace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article { test,author={Ed von Test}}"));

        List<BibEntry> parsed = result.getDatabase().getEntries();
        assertEquals(1, parsed.size());

        BibEntry e = parsed.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithNewlines() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article\n{\ntest,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithUnknownType() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@unknown{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("unknown", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithVeryLongType() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@thisIsALongStringToTestMaybeItIsToLongWhoKnowsNOTme{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("thisisalongstringtotestmaybeitistolongwhoknowsnotme", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryInParenthesis() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article(test,author={Ed von Test})"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    /**
     * Test for [ 1594123 ] Failure to import big numbers Issue Reported by Ulf Martin. SF Bugs #503, #495 are also
     * related
     */
    @Test
    public void parseRecognizesEntryWithBigNumbers() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05," + "isbn = 1234567890123456789,\n"
                + "isbn2 = {1234567890123456789},\n" + "small = 1234,\n" + "}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();

        assertEquals("article", e.getType());
        assertEquals("canh05", e.getCiteKey());
        assertEquals("1234567890123456789", e.getField("isbn"));
        assertEquals("1234567890123456789", e.getField("isbn2"));
        assertEquals("1234", e.getField("small"));
    }

    @Test
    public void parseRecognizesBibtexKeyWithSpecialCharacters() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{te_st:with-special(characters),author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();

        assertEquals("article", e.getType());
        assertEquals("te_st:with-special(characters)", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWhereLastFieldIsFinishedWithComma() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test},}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesMultipleEntries() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader(
                        "@article{canh05,"
                        + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n"
                        + "@inProceedings{foo,"
                        + "  author={Norton Bar}}"));
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

        BibtexEntryAssert.assertEquals(expected, parsed);
    }

    @Test
    public void parseSetsParsedSerialization() throws IOException {
        String firstEntry = "@article{canh05,"
                + "  author = {Crowston, K. and Annabi, H.},"
                + Globals.NEWLINE
                + "  title = {Title A}}"
                + Globals.NEWLINE;
        String secondEntry = "@inProceedings{foo," + "  author={Norton Bar}}";

        ParserResult result = BibtexParser.parse(new StringReader(firstEntry + secondEntry));

        for (BibEntry entry : result.getDatabase().getEntries()) {
            if (entry.getCiteKey().equals("canh05")) {
                assertEquals(firstEntry, entry.getParsedSerialization());
            } else {
                assertEquals(secondEntry, entry.getParsedSerialization());
            }
        }
    }

    @Test
    public void parseRecognizesMultipleEntriesOnSameLine() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05}" + "@inProceedings{foo}"));
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

        BibtexEntryAssert.assertEquals(expected, parsed);
    }

    @Test
    public void parseCombinesMultipleAuthorFields() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@article{test,author={Ed von Test},author={Second Author},author={Third Author}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test and Second Author and Third Author", e.getField("author"));
    }

    @Test
    public void parseCombinesMultipleEditorFields() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@article{test,editor={Ed von Test},editor={Second Author},editor={Third Author}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test and Second Author and Third Author", e.getField("editor"));
    }

    /**
     * Test for SF Bug #1269
     */
    @Test
    public void parseCombinesMultipleKeywordsFields() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@article{test,Keywords={Test},Keywords={Second Keyword},Keywords={Third Keyword}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Test, Second Keyword, Third Keyword", e.getField("keywords"));
    }

    @Test
    public void parseRecognizesHeaderButIgnoresEncoding() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(
                "This file was created with JabRef 2.1 beta 2."
                + "\n"
                + "Encoding: Cp1252"
                + "\n"
                + ""
                + "\n"
                + "@INPROCEEDINGS{CroAnnHow05,"
                + "\n"
                + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                + "\n"
                + "  title = {Effective work practices for floss development: A model and propositions},"
                + "\n"
                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                + "\n"
                + "  year = {2005},"
                + "\n"
                + "  owner = {oezbek},"
                + "\n"
                + "  timestamp = {2006.05.29},"
                + "\n"
                + "  url = {http://james.howison.name/publications.html}"
                + "\n"
                + "}))"));
        assertNull(result.getEncoding());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("inproceedings", e.getType());
        assertEquals(8, e.getFieldNames().size());
        assertEquals("CroAnnHow05", e.getCiteKey());
        assertEquals("Crowston, K. and Annabi, H. and Howison, J. and Masango, C.", e.getField("author"));
        assertEquals("Effective work practices for floss development: A model and propositions", e.getField("title"));
        assertEquals("Hawaii International Conference On System Sciences (HICSS)", e.getField("booktitle"));
        assertEquals("2005", e.getField("year"));
        assertEquals("oezbek", e.getField("owner"));
        assertEquals("2006.05.29", e.getField("timestamp"));
        assertEquals("http://james.howison.name/publications.html", e.getField("url"));
    }

    @Test
    public void parseRecognizesFormatedEntry() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(""
                + "@INPROCEEDINGS{CroAnnHow05," +
                "\n"
                + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                + "\n"
                + "  title = {Effective work practices for floss development: A model and propositions},"
                + "\n"
                + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                + "\n"
                + "  year = {2005},"
                + "\n"
                + "  owner = {oezbek},"
                + "\n"
                + "  timestamp = {2006.05.29},"
                + "\n"
                + "  url = {http://james.howison.name/publications.html}"
                + "\n"
                + "}))"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("inproceedings", e.getType());
        assertEquals(8, e.getFieldNames().size());
        assertEquals("CroAnnHow05", e.getCiteKey());
        assertEquals("Crowston, K. and Annabi, H. and Howison, J. and Masango, C.", e.getField("author"));
        assertEquals("Effective work practices for floss development: A model and propositions", e.getField("title"));
        assertEquals("Hawaii International Conference On System Sciences (HICSS)", e.getField("booktitle"));
        assertEquals("2005", e.getField("year"));
        assertEquals("oezbek", e.getField("owner"));
        assertEquals("2006.05.29", e.getField("timestamp"));
        assertEquals("http://james.howison.name/publications.html", e.getField("url"));
    }

    @Test
    public void parseRecognizesFieldValuesInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"Ed von Test\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesNumbersWithoutBracketsOrQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,year = 2005}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("2005", e.getField("year"));
    }

    @Test
    public void parseRecognizesUppercaseFields() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,AUTHOR={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    /**
     * Test for SF Bug #806
     */
    @Test
    public void parseRecognizesAbsoluteFile() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,file = {D:\\Documents\\literature\\Tansel-PRL2006.pdf}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("D:\\Documents\\literature\\Tansel-PRL2006.pdf", e.getField("file"));
    }

    /**
     * Test for SF Bug #48
     */
    @Test
    public void parseRecognizesDateFieldWithConcatenation() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,date = {1-4~} # nov}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("1-4~#nov#", e.getField("date"));
    }

    @Test
    public void parseReturnsEmptyListIfNoEntryRecognized() throws IOException {
        ParserResult result = BibtexParser
                .parse(new StringReader(
                        "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                        + "\n"
                        + "  title = {Effective work practices for floss development: A model and propositions},"
                        + "\n"
                        + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                        + "\n"
                        + "  year = {2005},"
                        + "\n"
                        + "  owner = {oezbek},"
                        + "\n"
                        + "  timestamp = {2006.05.29},"
                        + "\n"
                        + "  url = {http://james.howison.name/publications.html}"
                        + "\n"
                        + "}))"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseReturnsEmptyListIfNoEntryExistent() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("This was created with JabRef 2.1 beta 2." + "\n" + "Encoding: Cp1252" + "\n"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseRecognizesDuplicateBibtexKeys() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n" + "@inProceedings{canh05," + "  author={Norton Bar}}"));

        String[] duplicateKeys = result.getDuplicateKeys();
        assertEquals(1, duplicateKeys.length);
        assertEquals("canh05", duplicateKeys[0]);
    }

    @Test
    public void parseWarnsAboutEntryWithoutBibtexKey() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{,author={Ed von Test}}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracket() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author missing bracket}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    /**
     * Test for SF bug 482
     */
    @Test
    public void parseAddsEscapedOpenBracketToFieldValue() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,review={escaped \\{ bracket}}"));

        assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals("escaped \\{ bracket", e.getField("review"));
    }

    @Test
    public void parseAddsEscapedClosingBracketToFieldValue() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,review={escaped \\} bracket}}"));

        assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals("escaped \\} bracket", e.getField("review"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracketInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"author {missing bracket\"}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseIgnoresArbitraryContentAfterEntry() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author bracket }}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals("Size should be one, but was " + c.size(), 1, c.size());
        assertEquals("Epilog should be preserved", "}", result.getDatabase().getEpilog());
    }

    @Test
    public void parseWarnsAboutUnmatchedContentInEntry() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author bracket }, too much}"));

        assertTrue("There should be warnings", result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals("Size should be zero, but was " + c.size(), 0, c.size());
    }

    @Test
    @Ignore("Ignoring because this is an edge case")
    public void parseWarnsAboutUnmatchedContentInEntryWithoutComma() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author bracket } too much}"));

        assertTrue("There should be warnings", result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals("Size should be zero, but was " + c.size(), 0, c.size());
    }

    @Test
    public void parseAcceptsEntryWithAtSymbolInBrackets() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author @ good}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        List<BibEntry> entries = new ArrayList<>(1);
        entries.addAll(c);

        assertEquals(1, entries.size());
        assertEquals("author @ good", entries.get(0).getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithAtSymbolInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"author @ good\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("author @ good", e.getField("author"));
    }

    @Test
    public void parseRecognizesFieldsWithBracketsEnclosedInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"Test {Ed {von} Test}\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Test {Ed {von} Test}", e.getField("author"));
    }

    @Test
    public void parseRecognizesFieldsWithEscapedQuotationMarks() throws IOException {

        // Quotes in fields of the form key = "value" have to be escaped by putting them into braces
        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"Test {\" Test}\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Test {\" Test}", e.getField("author"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithFieldsThatAreNotSeperatedByComma() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test} year=2005}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(0, c.size());
    }

    @Test
    public void parseIgnoresAndWarnsAboutCorruptedEntryButRecognizeOthers() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(
                "@article{test,author={author missing bracket}" + "@article{test,author={Ed von Test}}"));

        assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    /**
     * Test for SF Bug #1283
     */
    @Test
    public void parseRecognizesMonthFieldsWithFollowingComma() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test},month={8,}},"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(3, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
        assertEquals("8,", e.getField("month"));
    }

    @Test
    public void parseRecognizesPreamble() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble{some text and \\latex}"));
        assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesUppercasePreamble() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@PREAMBLE{some text and \\latex}"));
        assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleWithWhitespace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble {some text and \\latex}"));
        assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleInParenthesis() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble(some text and \\latex)"));
        assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleWithConcatenation() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble{\"some text\" # \"and \\latex\"}"));
        assertEquals("\"some text\" # \"and \\latex\"", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesString() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}"));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseSavesOneNewlineAfterStringInParsedSerialization() throws IOException {

        String string = "@string{bourdieu = {Bourdieu, Pierre}}" + Globals.NEWLINE;
        ParserResult result = BibtexParser.parse(new StringReader(string + Globals.NEWLINE + Globals.NEWLINE));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals(string, s.getParsedSerialization());
    }

    @Test
    public void parseRecognizesStringWithWhitespace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@string {bourdieu = {Bourdieu, Pierre}}"));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseRecognizesStringInParenthesis() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@string(bourdieu = {Bourdieu, Pierre})"));
        assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseRecognizesMultipleStrings() throws IOException {

        ParserResult result = BibtexParser
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

        ParserResult result = BibtexParser.parse(new StringReader(
          ""
        + "@string{bourdieu = {Bourdieu, Pierre}}"
        + "@book{bourdieu-2002-questions-sociologie, "
        + "	Address = {Paris},"
        + "	Author = bourdieu,"
        + "	Isbn = 2707318256,"
        + "	Publisher = {Minuit},"
        + "	Title = {Questions de sociologie},"
        + "	Year = 2002"
        + "}"));

        assertEquals(1, result.getDatabase().getStringCount());
        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        assertEquals("bourdieu", s.getName());
        assertEquals("Bourdieu, Pierre", s.getContent());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();

        assertEquals("book", e.getType());
        assertEquals("bourdieu-2002-questions-sociologie", e.getCiteKey());
        assertEquals("Paris", e.getField("address"));
        assertEquals("#bourdieu#", e.getField("author"));
        assertEquals("2707318256", e.getField("isbn"));
        assertEquals("Minuit", e.getField("publisher"));
        assertEquals("Questions de sociologie", e.getField("title"));
        assertEquals("2002", e.getField("year"));
    }

    @Test
    public void parseWarnsAboutStringsWithSameNameAndOnlyKeepsOne() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}" + "@string{bourdieu = {Other}}"));
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getDatabase().getStringCount());
    }

    @Test
    public void parseIgnoresComments() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@comment{some text and \\latex}"));
        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresUpercaseComments() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@COMMENT{some text and \\latex}"));
        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresCommentsBeforeEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@comment{some text and \\latex}" + "@article{test,author={Ed von Test}}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresCommentsAfterEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "@comment{some text and \\latex}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresText() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("comment{some text and \\latex"));
        assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresTextBeforeEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("comment{some text and \\latex" + "@article{test,author={Ed von Test}}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresTextAfterEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "comment{some text and \\latex"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals(2, e.getFieldNames().size());
        assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseConvertsNewlineToSpace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,a = {a\nb}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals("a b", e.getField("a"));
    }

    @Test
    public void parseConvertsMultipleNewlinesToSpace() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,a = {a\n\nb}," + "b = {a\n \nb}," + "c = {a \n \n b}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals("a b", e.getField("a"));
        assertEquals("a b", e.getField("b"));
        assertEquals("a b", e.getField("c"));
    }

    @Test
    public void parseConvertsTabToSpace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,a = {a\tb}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals("a b", e.getField("a"));
    }

    @Test
    public void parseConvertsMultipleTabsToSpace() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,a = {a\t\tb}," + "b = {a\t \tb}," + "c = {a \t \t b}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals("a b", e.getField("a"));
        assertEquals("a b", e.getField("b"));
        assertEquals("a b", e.getField("c"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    public void parsePreservesMultipleSpacesInFileField() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,file = {ups  sala}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals("ups  sala", e.getField("file"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    @Ignore("Ignoring, since the parser is not responsible for fixing the content. This should be done later")
    public void parseRemovesTabsInFileField() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,file = {ups  \tsala}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals("ups  sala", e.getField("file"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    @Ignore("Ignoring, since the parser is not responsible for fixing the content. This should be done later")
    public void parseRemovesNewlineInFileField() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,file = {ups \n\tsala}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        assertEquals("ups  sala", e.getField("file"));
    }

    /**
     * Test for #650
     */
    @Test
    public void parseHandlesAccentsCorrectly() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author = {H\'{e}lne Fiaux}}"));
        assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals("H\'{e}lne Fiaux", e.getField("author"));
    }

    /**
     * Test for #669
     */
    @Test
    public void parsePreambleAndEntryWithoutNewLine() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@preamble{some text and \\latex}@article{test,author = {H\'{e}lne Fiaux}}"));
        assertFalse(result.hasWarnings());

        assertEquals("some text and \\latex", result.getDatabase().getPreamble());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals("article", e.getType());
        assertEquals("test", e.getCiteKey());
        assertEquals("H\'{e}lne Fiaux", e.getField("author"));
    }

    /**
     * Test for #669
     */
    @Test
    public void parseFileHeaderAndPreambleWithoutNewLine() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("% Encoding: US-ASCII@preamble{some text and \\latex}"));
        assertFalse(result.hasWarnings());

        assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseSavesEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = BibtexParser.parse(new StringReader(testEntry));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry, e.getParsedSerialization());
    }

    @Test
    public void parseSavesOneNewlineAfterEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = BibtexParser.parse(new StringReader(testEntry + Globals.NEWLINE + Globals.NEWLINE));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry + Globals.NEWLINE, e.getParsedSerialization());
    }

    @Test
    public void parseSavesNewlinesBeforeEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = BibtexParser
                .parse(new StringReader(Globals.NEWLINE + Globals.NEWLINE + Globals.NEWLINE + testEntry));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(Globals.NEWLINE + Globals.NEWLINE + Globals.NEWLINE + testEntry, e.getParsedSerialization());
    }

    @Test
    public void parseSavesOnlyRealNewlinesBeforeEntryInParsedSerialization() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = BibtexParser.parse(
                new StringReader("%Encoding: no" + Globals.NEWLINE + Globals.NEWLINE + Globals.NEWLINE + testEntry));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(Globals.NEWLINE + Globals.NEWLINE + testEntry, e.getParsedSerialization());
    }

    @Test
    public void parseSavesNewlinesBetweenEntriesInParsedSerialization() throws IOException {
        String testEntryOne = "@article{test1,author={Ed von Test}}";
        String testEntryTwo = "@article{test2,author={Ed von Test}}";
        ParserResult result = BibtexParser.parse(
                new StringReader(testEntryOne + Globals.NEWLINE + Globals.NEWLINE + Globals.NEWLINE + testEntryTwo));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(2, c.size());

        Iterator<BibEntry> i = c.iterator();
        BibEntry a = i.next();
        BibEntry b = i.next();

        // Sort them because we can't be sure about the order
        if (a.getCiteKey().equals("test2")) {
            BibEntry tmp = a;
            a = b;
            b = tmp;
        }

        assertEquals(testEntryOne + Globals.NEWLINE, a.getParsedSerialization());
        assertEquals(Globals.NEWLINE + Globals.NEWLINE + testEntryTwo, b.getParsedSerialization());
    }

    @Test
    public void parseIgnoresWhitespaceInEpilogue() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("   " + Globals.NEWLINE));

        assertEquals("", result.getDatabase().getEpilog());
    }

    @Test
    public void parseIgnoresWhitespaceInEpilogueAfterEntry() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = BibtexParser.parse(new StringReader(
                testEntry + Globals.NEWLINE + Globals.NEWLINE + Globals.NEWLINE + "  " + Globals.NEWLINE));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry + Globals.NEWLINE, e.getParsedSerialization());
        assertEquals("", result.getDatabase().getEpilog());
    }

    @Test
    public void parseTrimsWhitespaceInEpilogueAfterEntry() throws IOException {
        String testEntry = "@article{test,author={Ed von Test}}";
        ParserResult result = BibtexParser.parse(new StringReader(
                testEntry + Globals.NEWLINE + Globals.NEWLINE + Globals.NEWLINE + " epilogue " + Globals.NEWLINE));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        assertEquals(testEntry + Globals.NEWLINE, e.getParsedSerialization());
        assertEquals("epilogue", result.getDatabase().getEpilog());
    }

    @Test
    public void parseRecognizesSaveActionsAfterEntry() throws IOException {
        BibtexParser parser = new BibtexParser(
                new StringReader("@InProceedings{6055279,\n" + "  Title                    = {Educational session 1},\n"
                        + "  Booktitle                = {Custom Integrated Circuits Conference (CICC), 2011 IEEE},\n"
                        + "  Year                     = {2011},\n" + "  Month                    = {Sept},\n"
                        + "  Pages                    = {1-7},\n"
                        + "  Abstract                 = {Start of the above-titled section of the conference proceedings record.},\n"
                        + "  DOI                      = {10.1109/CICC.2011.6055279},\n"
                        + "  ISSN                     = {0886-5930}\n" + "}\n" + "\n"
                        + "@comment{jabref-meta: saveActions:enabled;title[lower_case]}"));

        ParserResult parserResult = parser.parse();

        List<String> saveActions = parserResult.getMetaData().getData(MetaData.SAVE_ACTIONS);

        assertEquals("enabled", saveActions.get(0));
        assertEquals("title[lower_case]", saveActions.get(1));
    }

    @Test
    public void integrationTestSaveActions() throws IOException {
        BibtexParser parser = new BibtexParser(
                new StringReader("@comment{jabref-meta: saveActions:enabled;title[lower_case]}"));

        ParserResult parserResult = parser.parse();
        FieldFormatterCleanups saveActions = parserResult.getMetaData().getSaveActions();

        assertTrue(saveActions.isEnabled());
        assertEquals(Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())),
                saveActions.getConfiguredActions());
    }

    @Test
    public void integrationTestCustomEntryType() throws IOException {
        ParserResult result = BibtexParser.parse(
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
        ParserResult result = BibtexParser.parse(new StringReader(
                "@Comment{jabref-meta: saveOrderConfig:specified;author;false;year;true;abstract;false;}"));

        Optional<SaveOrderConfig> saveOrderConfig = result.getMetaData().getSaveOrderConfig();

        assertEquals(new SaveOrderConfig(false, new SaveOrderConfig.SortCriterion("author", false),
                new SaveOrderConfig.SortCriterion("year", true), new SaveOrderConfig.SortCriterion("abstract", false)),
                saveOrderConfig.get());
    }

    @Test
    public void integrationTestCustomKeyPattern() throws IOException {
        ParserResult result = BibtexParser
                .parse(new StringReader(
                        "@comment{jabref-meta: keypattern_article:articleTest;}"
                        + Globals.NEWLINE
                        + "@comment{jabref-meta: keypatterndefault:test;}"));

        AbstractLabelPattern labelPattern = result.getMetaData().getLabelPattern();

        AbstractLabelPattern expectedPattern = new DatabaseLabelPattern();
        expectedPattern.setDefaultValue("test");
        expectedPattern.addLabelPattern("article", "articleTest");

        assertEquals(expectedPattern, labelPattern);
    }

    @Test
    public void integrationTestBiblatexMode() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@comment{jabref-meta: databaseType:biblatex;}"));

        Optional<BibDatabaseMode> mode = result.getMetaData().getMode();

        assertEquals(BibDatabaseMode.BIBLATEX, mode.get());
    }

    @Test
    public void integrationTestGroupTree() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(
                "@comment{jabref-meta: groupsversion:3;}"
                + Globals.NEWLINE +
                "@comment{jabref-meta: groupstree:"
                + Globals.NEWLINE
                + "0 AllEntriesGroup:;"
                + Globals.NEWLINE
                + "1 KeywordGroup:Fréchet\\;0\\;keywords\\;FrechetSpace\\;0\\;1\\;;"
                + Globals.NEWLINE
                + "1 KeywordGroup:Invariant theory\\;0\\;keywords\\;GIT\\;0\\;0\\;;"
                + "}"));

        GroupTreeNode root = result.getMetaData().getGroups();

        assertEquals(new AllEntriesGroup(), root.getGroup());
        assertEquals(2, root.getChildCount());
        assertEquals(
                new KeywordGroup("Fréchet", "keywords", "FrechetSpace", false, true, GroupHierarchyType.INDEPENDENT),
                ((GroupTreeNode) root.getChildAt(0)).getGroup());
        assertEquals(
                new KeywordGroup("Invariant theory", "keywords", "GIT", false, false, GroupHierarchyType.INDEPENDENT),
                ((GroupTreeNode) root.getChildAt(1)).getGroup());
    }

    @Test
    public void integrationTestProtectedFlag() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@comment{jabref-meta: protectedFlag:true;}"));

        assertTrue(result.getMetaData().isProtected());
    }

    @Test
    public void integrationTestContentSelectors() throws IOException {
        ParserResult result = BibtexParser
                .parse(new StringReader("@comment{jabref-meta: selector_title:testWord;word2;}"));

        assertEquals(Arrays.asList("testWord", "word2"), result.getMetaData().getContentSelectors("title"));
    }

    @Test
    public void integrationTestFileDirectories() throws IOException {
        ParserResult result = BibtexParser
                .parse(new StringReader(
                        "@comment{jabref-meta: fileDirectory:\\\\Literature\\\\;}"
                        + "@comment{jabref-meta: fileDirectory-defaultOwner-user:D:\\\\Documents;}"));

        assertEquals("\\Literature\\", result.getMetaData().getDefaultFileDirectory().get());
        assertEquals("D:\\Documents", result.getMetaData().getUserFileDirectory("defaultOwner-user").get());
    }

    @Test
    public void parseReturnsEntriesInSameOrder() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(
                "@article{a}" + Globals.NEWLINE + "@article{b}" + Globals.NEWLINE + "@inProceedings{c}"));

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

        BibtexEntryAssert.assertEquals(expected, result.getDatabase().getEntries());
    }
}
