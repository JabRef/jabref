package net.sf.jabref.importer.fileformat;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.BibtexString;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Test the BibtexParser
 *
 * @author Christopher Oezbek <oezi@oezi.de>
 * @version $revision: 1.1$ $date: $
 */
public class BibtexParserTest {

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test(expected = NullPointerException.class)
    public void initalizationWithNullThrowsNullPointerException() {
        new BibtexParser(null);
    }

    @Test
    public void fromStringRecognizesEntry() throws Exception {
        Collection<BibEntry> c = BibtexParser.fromString("@article{test,author={Ed von Test}}");
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void fromStringReturnsEmptyListFromEmptyString() {
        Collection<BibEntry> c = BibtexParser.fromString("");
        Assert.assertNotNull(c);
        Assert.assertEquals(0, c.size());
    }

    @Test
    public void fromStringReturnsEmptyListIfNoEntryRecognized() {
        Collection<BibEntry> c = BibtexParser.fromString("@@article@@{{{{{{}");
        Assert.assertNotNull(c);
        Assert.assertEquals(0, c.size());
    }

    @Test
    public void singleFromStringRecognizesEntry() {
        BibEntry e = BibtexParser.singleFromString(
                "@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n" + "  title = {Title A}}\n");

        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("canh05", e.getCiteKey());
        Assert.assertEquals("Crowston, K. and Annabi, H.", e.getField("author"));
        Assert.assertEquals("Title A", e.getField("title"));
    }

    @Test
    public void singleFromStringRecognizesEntryInMultiple() {
        BibEntry e = BibtexParser.singleFromString("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                + "  title = {Title A}}\n" + "@inProceedings{foo," + "  author={Norton Bar}}");

        Assert.assertTrue(e.getCiteKey().equals("canh05") || e.getCiteKey().equals("foo"));
    }

    @Test
    public void singleFromStringReturnsNullFromEmptyString() {
        BibEntry e = BibtexParser.singleFromString("");
        Assert.assertNull(e);
    }

    @Test
    public void singleFromStringReturnsNullIfNoEntryRecognized() {
        BibEntry e = BibtexParser.singleFromString("@@article@@{{{{{{}");
        Assert.assertNull(e);
    }

    @Test
    public void parseTwoTimesReturnsSameResult() throws IOException {

        BibtexParser parser = new BibtexParser(new StringReader("@article{test,author={Ed von Test}}"));
        ParserResult result = parser.parse();

        Assert.assertEquals(result, parser.parse());
    }

    @Test
    public void parseRecognizesEntry() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(
                "@article{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryOnlyWithKey() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(
                "@article{test}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
    }

    @Test
    public void parseRecognizesEntryWithWhitespaceAtBegining() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(" @article{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithWhitespace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article { test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithNewlines() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article\n{\ntest,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithUnknownType() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@unknown{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("unknown", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithVeryLongType() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@thisIsALongStringToTestMaybeItIsToLongWhoKnowsNOTme{test,author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("thisisalongstringtotestmaybeitistolongwhoknowsnotme", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryInParenthesis() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article(test,author={Ed von Test})"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
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

        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("canh05", e.getCiteKey());
        Assert.assertEquals("1234567890123456789", e.getField("isbn"));
        Assert.assertEquals("1234567890123456789", e.getField("isbn2"));
        Assert.assertEquals("1234", e.getField("small"));
    }

    @Test
    public void parseRecognizesBibtexKeyWithSpecialCharacters() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{te_st:with-special(characters),author={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();

        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("te_st:with-special(characters)", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesEntryWhereLastFieldIsFinishedWithComma() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test},}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesMultipleEntries() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n" + "@inProceedings{foo," + "  author={Norton Bar}}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(2, c.size());

        Iterator<BibEntry> i = c.iterator();
        BibEntry a = i.next();
        BibEntry b = i.next();

        // Sort them because we can't be sure about the order
        if (a.getCiteKey().equals("foo")) {
            BibEntry tmp = a;
            a = b;
            b = tmp;
        }

        Assert.assertEquals("article", a.getType());
        Assert.assertEquals("canh05", a.getCiteKey());
        Assert.assertEquals("Crowston, K. and Annabi, H.", a.getField("author"));
        Assert.assertEquals("Title A", a.getField("title"));

        Assert.assertEquals("inproceedings", b.getType());
        Assert.assertEquals("foo", b.getCiteKey());
        Assert.assertEquals("Norton Bar", b.getField("author"));
    }

    @Test
    public void parseCombinesMultipleAuthorFields() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@article{test,author={Ed von Test},author={Second Author},author={Third Author}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test and Second Author and Third Author", e.getField("author"));
    }

    @Test
    public void parseCombinesMultipleEditorFields() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@article{test,editor={Ed von Test},editor={Second Author},editor={Third Author}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test and Second Author and Third Author", e.getField("editor"));
    }

    /**
     * Test for SF Bug #1269
     */
    @Test
    public void parseCombinesMultipleKeywordsFields() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@article{test,Keywords={Test},Keywords={Second Keyword},Keywords={Third Keyword}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Test, Second Keyword, Third Keyword", e.getField("keywords"));
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
                        + "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
                        + "  timestamp = {2006.05.29}," + "\n"
                        + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"
        ));
        Assert.assertNull(result.getEncoding());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("inproceedings", e.getType());
        Assert.assertEquals(8, e.getFieldNames().size());
        Assert.assertEquals("CroAnnHow05", e.getCiteKey());
        Assert.assertEquals("Crowston, K. and Annabi, H. and Howison, J. and Masango, C.", e.getField("author"));
        Assert.assertEquals("Effective work practices for floss development: A model and propositions",
                e.getField("title"));
        Assert.assertEquals("Hawaii International Conference On System Sciences (HICSS)", e.getField("booktitle"));
        Assert.assertEquals("2005", e.getField("year"));
        Assert.assertEquals("oezbek", e.getField("owner"));
        Assert.assertEquals("2006.05.29", e.getField("timestamp"));
        Assert.assertEquals("http://james.howison.name/publications.html", e.getField("url"));
    }

    @Test
    public void parseRecognizesFormatedEntry() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(
                "@INPROCEEDINGS{CroAnnHow05,"
                        + "\n"
                        + "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                        + "\n"
                        + "  title = {Effective work practices for floss development: A model and propositions},"
                        + "\n"
                        + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                        + "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
                        + "  timestamp = {2006.05.29}," + "\n"
                        + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"
        ));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("inproceedings", e.getType());
        Assert.assertEquals(8, e.getFieldNames().size());
        Assert.assertEquals("CroAnnHow05", e.getCiteKey());
        Assert.assertEquals("Crowston, K. and Annabi, H. and Howison, J. and Masango, C.", e.getField("author"));
        Assert.assertEquals("Effective work practices for floss development: A model and propositions",
                e.getField("title"));
        Assert.assertEquals("Hawaii International Conference On System Sciences (HICSS)", e.getField("booktitle"));
        Assert.assertEquals("2005", e.getField("year"));
        Assert.assertEquals("oezbek", e.getField("owner"));
        Assert.assertEquals("2006.05.29", e.getField("timestamp"));
        Assert.assertEquals("http://james.howison.name/publications.html", e.getField("url"));
    }

    @Test
    public void parseRecognizesFieldValuesInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"Ed von Test\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseRecognizesNumbersWithoutBracketsOrQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,year = 2005}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("2005", e.getField("year"));
    }

    @Test
    public void parseRecognizesUppercaseFields() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,AUTHOR={Ed von Test}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    /**
     * Test for SF Bug #806
     */
    @Test
    public void parseRecognizesAbsoluteFile() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,file = {D:\\Documents\\literature\\Tansel-PRL2006.pdf}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("D:\\Documents\\literature\\Tansel-PRL2006.pdf", e.getField("file"));
    }

    /**
     * Test for SF Bug #48
     */
    @Test
    public void parseRecognizesDateFieldWithConcatenation() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,date = {1-4~} # nov}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("1-4~#nov#", e.getField("date"));
    }

    @Test
    public void parseReturnsEmptyListIfNoEntryRecognized() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader(
                "  author = {Crowston, K. and Annabi, H. and Howison, J. and Masango, C.},"
                        + "\n"
                        + "  title = {Effective work practices for floss development: A model and propositions},"
                        + "\n"
                        + "  booktitle = {Hawaii International Conference On System Sciences (HICSS)},"
                        + "\n" + "  year = {2005}," + "\n" + "  owner = {oezbek}," + "\n"
                        + "  timestamp = {2006.05.29}," + "\n"
                        + "  url = {http://james.howison.name/publications.html}" + "\n" + "}))"
        ));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(0, c.size());
    }

    @Test
    public void parseReturnsEmptyListIfNoEntryExistent() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(
                "This was created with JabRef 2.1 beta 2." + "\n" + "Encoding: Cp1252" + "\n"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(0, c.size());
    }

    @Test
    public void parseRecognizesDuplicateBibtexKeys() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{canh05," + "  author = {Crowston, K. and Annabi, H.},\n"
                        + "  title = {Title A}}\n" + "@inProceedings{canh05," + "  author={Norton Bar}}"));

        String[] duplicateKeys = result.getDuplicateKeys();
        Assert.assertEquals(1, duplicateKeys.length);
        Assert.assertEquals("canh05", duplicateKeys[0]);
    }

    @Test
    public void parseWarnsAboutEntryWithoutBibtexKey() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{,author={Ed von Test}}"));

        Assert.assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracket() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author missing bracket}"));

        Assert.assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(0, c.size());
    }

    /**
     * Test for SF bug 482
     */
    @Test
    public void parseAddsEscapedOpenBracketToFieldValue() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,review={escaped \\{ bracket}}"));

        Assert.assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals("escaped \\{ bracket", e.getField("review"));
    }

    @Test
    public void parseAddsEscapedClosingBracketToFieldValue() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,review={escaped \\} bracket}}"));

        Assert.assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals("escaped \\} bracket", e.getField("review"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithUnmatchedOpenBracketInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"author {missing bracket\"}"));

        Assert.assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(0, c.size());
    }

    @Test
    @Ignore
    public void parseIgnoresAndWarnsAboutEntryWithUnmatchedClosingBracket() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author bracket } to much}}"));

        Assert.assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals("Size should be zero, but was " + c.size(), 0, c.size());
    }

    @Test
    public void parseAcceptsEntryWithAtSymbolInBrackets() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author @ good}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        List<BibEntry> entries = new ArrayList<>(1);
        entries.addAll(c);

        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("author @ good", entries.get(0).getField("author"));
    }

    @Test
    public void parseRecognizesEntryWithAtSymbolInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"author @ good\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("author @ good", e.getField("author"));
    }

    @Test
    public void parseRecognizesFieldsWithBracketsEnclosedInQuotationMarks() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"Test {Ed {von} Test}\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Test {Ed {von} Test}", e.getField("author"));
    }

    @Test
    public void parseRecognizesFieldsWithEscapedQuotationMarks() throws IOException {

        // Quotes in fields of the form key = "value" have to be escaped by putting them into braces
        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author=\"Test {\" Test}\"}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Test {\" Test}", e.getField("author"));
    }

    @Test
    public void parseIgnoresAndWarnsAboutEntryWithFieldsThatAreNotSeperatedByComma() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test} year=2005}"));

        Assert.assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(0, c.size());
    }

    @Test
    public void parseIgnoresAndWarnsAboutCorruptedEntryButRecognizeOthers() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={author missing bracket}" + "@article{test,author={Ed von Test}}"));

        Assert.assertTrue(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    /**
     * Test for SF Bug #1283
     */
    @Test
    @Ignore
    public void parseRecognizesMonthFieldsWithFollowingComma() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author={Ed von Test}},month={8,},"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals(BibtexEntryTypes.ARTICLE, e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(3, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
        Assert.assertEquals("8,", e.getField("month"));
    }

    @Test
    public void parseRecognizesPreamble() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble{some text and \\latex}"));
        Assert.assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesUppercasePreamble() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@PREAMBLE{some text and \\latex}"));
        Assert.assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleWithWhitespace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble {some text and \\latex}"));
        Assert.assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleInParenthesis() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble(some text and \\latex)"));
        Assert.assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesPreambleWithConcatenation() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@preamble{\"some text\" # \"and \\latex\"}"));
        Assert.assertEquals("\"some text\" # \"and \\latex\"", result.getDatabase().getPreamble());
    }

    @Test
    public void parseRecognizesString() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}"));
        Assert.assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        Assert.assertEquals("bourdieu", s.getName());
        Assert.assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseRecognizesStringWithWhitespace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@string {bourdieu = {Bourdieu, Pierre}}"));
        Assert.assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        Assert.assertEquals("bourdieu", s.getName());
        Assert.assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseRecognizesStringInParenthesis() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@string(bourdieu = {Bourdieu, Pierre})"));
        Assert.assertEquals(1, result.getDatabase().getStringCount());

        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        Assert.assertEquals("bourdieu", s.getName());
        Assert.assertEquals("Bourdieu, Pierre", s.getContent());
    }

    @Test
    public void parseRecognizesMultipleStrings() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}" + "@string{adieu = {Adieu, Pierre}}"));

        Assert.assertEquals(2, result.getDatabase().getStringCount());
        Iterator<BibtexString> iterator = result.getDatabase().getStringValues().iterator();
        BibtexString s = iterator.next();
        BibtexString t = iterator.next();

        // Sort them because we can't be sure about the order
        if (s.getName().equals("adieu")) {
            BibtexString tmp = s;
            s = t;
            t = tmp;
        }

        Assert.assertEquals("bourdieu", s.getName());
        Assert.assertEquals("Bourdieu, Pierre", s.getContent());
        Assert.assertEquals("adieu", t.getName());
        Assert.assertEquals("Adieu, Pierre", t.getContent());
    }

    @Test
    public void parseRecognizesStringAndEntry() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader(""
                + "@string{bourdieu = {Bourdieu, Pierre}}"
                + "@book{bourdieu-2002-questions-sociologie, " + "	Address = {Paris},"
                + "	Author = bourdieu," + "	Isbn = 2707318256," + "	Publisher = {Minuit},"
                + "	Title = {Questions de sociologie}," + "	Year = 2002" + "}"));

        Assert.assertEquals(1, result.getDatabase().getStringCount());
        BibtexString s = result.getDatabase().getStringValues().iterator().next();
        Assert.assertEquals("bourdieu", s.getName());
        Assert.assertEquals("Bourdieu, Pierre", s.getContent());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();

        Assert.assertEquals("book", e.getType());
        Assert.assertEquals("bourdieu-2002-questions-sociologie", e.getCiteKey());
        Assert.assertEquals("Paris", e.getField("address"));
        Assert.assertEquals("#bourdieu#", e.getField("author"));
        Assert.assertEquals("2707318256", e.getField("isbn"));
        Assert.assertEquals("Minuit", e.getField("publisher"));
        Assert.assertEquals("Questions de sociologie", e.getField("title"));
        Assert.assertEquals("2002", e.getField("year"));
    }

    @Test
    public void parseWarnsAboutStringsWithSameNameAndOnlyKeepsOne() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@string{bourdieu = {Bourdieu, Pierre}}" + "@string{bourdieu = {Other}}"));
        Assert.assertTrue(result.hasWarnings());
        Assert.assertEquals(1, result.getDatabase().getStringCount());
    }

    @Test
    public void parseIgnoresComments() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@comment{some text and \\latex}"));
        Assert.assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresUpercaseComments() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@COMMENT{some text and \\latex}"));
        Assert.assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresCommentsBeforeEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@comment{some text and \\latex}" + "@article{test,author={Ed von Test}}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresCommentsAfterEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "@comment{some text and \\latex}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresText() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("comment{some text and \\latex"));
        Assert.assertEquals(0, result.getDatabase().getEntries().size());
    }

    @Test
    public void parseIgnoresTextBeforeEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("comment{some text and \\latex" + "@article{test,author={Ed von Test}}"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseIgnoresTextAfterEntry() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,author={Ed von Test}}" + "comment{some text and \\latex"));
        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals(2, e.getFieldNames().size());
        Assert.assertEquals("Ed von Test", e.getField("author"));
    }

    @Test
    public void parseConvertsNewlineToSpace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,a = {a\nb}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        Assert.assertEquals("a b", e.getField("a"));
    }

    @Test
    public void parseConvertsMultipleNewlinesToSpace() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,a = {a\n\nb}," + "b = {a\n \nb}," + "c = {a \n \n b}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        Assert.assertEquals("a b", e.getField("a"));
        Assert.assertEquals("a b", e.getField("b"));
        Assert.assertEquals("a b", e.getField("c"));
    }

    @Test
    public void parseConvertsTabToSpace() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,a = {a\tb}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        Assert.assertEquals("a b", e.getField("a"));
    }

    @Test
    public void parseConvertsMultipleTabsToSpace() throws IOException {

        ParserResult result = BibtexParser
                .parse(new StringReader("@article{test,a = {a\t\tb}," + "b = {a\t \tb}," + "c = {a \t \t b}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        Assert.assertEquals("a b", e.getField("a"));
        Assert.assertEquals("a b", e.getField("b"));
        Assert.assertEquals("a b", e.getField("c"));
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
        Assert.assertEquals("ups  sala", e.getField("file"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    @Ignore
    public void parseRemovesTabsInFileField() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,file = {ups  \tsala}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        Assert.assertEquals("ups  sala", e.getField("file"));
    }

    /**
     * Test for [2022983]
     *
     * @author Uwe Kuehn
     * @author Andrei Haralevich
     */
    @Test
    @Ignore
    public void parseRemovesNewlineInFileField() throws IOException {
        ParserResult result = BibtexParser.parse(new StringReader("@article{canh05,file = {ups \n\tsala}}"));

        Collection<BibEntry> c = result.getDatabase().getEntries();
        BibEntry e = c.iterator().next();
        Assert.assertEquals("ups  sala", e.getField("file"));
    }

    /**
     * Test for #650
     */
    @Test
    public void parseHandlesAccentsCorrectly() throws IOException {

        ParserResult result = BibtexParser.parse(new StringReader("@article{test,author = {H\'{e}lne Fiaux}}"));
        Assert.assertFalse(result.hasWarnings());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals("H\'{e}lne Fiaux", e.getField("author"));
    }

    /**
     * Test for #669
     */
    @Test
    public void parsePreambleAndEntryWithoutNewLine() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("@preamble{some text and \\latex}@article{test,author = {H\'{e}lne Fiaux}}"));
        Assert.assertFalse(result.hasWarnings());

        Assert.assertEquals("some text and \\latex", result.getDatabase().getPreamble());

        Collection<BibEntry> c = result.getDatabase().getEntries();
        Assert.assertEquals(1, c.size());

        BibEntry e = c.iterator().next();
        Assert.assertEquals("article", e.getType());
        Assert.assertEquals("test", e.getCiteKey());
        Assert.assertEquals("H\'{e}lne Fiaux", e.getField("author"));
    }

    /**
     * Test for #669
     */
    @Test
    public void parseFileHeaderAndPreambleWithoutNewLine() throws IOException {

        ParserResult result = BibtexParser.parse(
                new StringReader("% Encoding: US-ASCII@preamble{some text and \\latex}"));
        Assert.assertFalse(result.hasWarnings());

        Assert.assertEquals("some text and \\latex", result.getDatabase().getPreamble());
    }
}