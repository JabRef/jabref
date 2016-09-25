package net.sf.jabref.logic.integrity;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;

public class IntegrityCheckTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testUrlChecks() {
        assertCorrect(createContext("url", "http://www.google.com"));
        assertCorrect(createContext("url", "file://c:/asdf/asdf"));
        assertCorrect(createContext("url", "http://scikit-learn.org/stable/modules/ensemble.html#random-forests"));

        assertWrong(createContext("url", "www.google.com"));
        assertWrong(createContext("url", "google.com"));
        assertWrong(createContext("url", "c:/asdf/asdf"));
    }

    @Test
    public void testYearChecks() {
        assertCorrect(createContext("year", "2014"));
        assertCorrect(createContext("year", "1986"));
        assertCorrect(createContext("year", "around 1986"));
        assertCorrect(createContext("year", "(around 1986)"));
        assertCorrect(createContext("year", "1986,"));
        assertCorrect(createContext("year", "1986}%"));
        assertCorrect(createContext("year", "1986(){},.;!?<>%&$"));
        assertWrong(createContext("year", "abc"));
        assertWrong(createContext("year", "86"));
        assertWrong(createContext("year", "204"));
        assertWrong(createContext("year", "1986a"));
        assertWrong(createContext("year", "(1986a)"));
        assertWrong(createContext("year", "1986a,"));
        assertWrong(createContext("year", "1986}a%"));
        assertWrong(createContext("year", "1986a(){},.;!?<>%&$"));
    }

    @Test
    public void testEditionChecks() {
        assertCorrect(withMode(createContext("edition", "Second"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("edition", "Third"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("edition", "second"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("edition", "2"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("edition", "2nd"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("edition", "2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("edition", "10"), BibDatabaseMode.BIBLATEX));
        assertCorrect(
                withMode(createContext("edition", "Third, revised and expanded edition"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("edition", "Edition 2000"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("edition", "2nd"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    public void testNoteChecks() {
        assertCorrect(withMode(createContext("note", "Lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("note", "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("note", "lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("note", "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("note", "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    public void testHowpublishedChecks() {
        assertCorrect(withMode(createContext("howpublished", "Lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("howpublished", "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("howpublished", "lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("howpublished", "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("howpublished", "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    public void testMonthChecks() {
        assertCorrect(withMode(createContext("month", "#mar#"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("month", "#dec#"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("month", "#bla#"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("month", "Dec"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("month", "December"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("month", "Lorem"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("month", "10"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("month", "1"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("month", "10"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("month", "#jan#"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("month", "jan"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("month", "january"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("month", "January"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("month", "Lorem"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    public void testJournaltitleChecks() {
        assertCorrect(withMode(createContext("journaltitle", "A journal"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("journaltitle", "A journal"), BibDatabaseMode.BIBTEX));
    }

    @Test
    public void testBibtexkeyChecks() {
        assertCorrect(createContext("bibtexkey", "Knuth2014"));
    }

    @Test
    public void testBracketChecks() {
        assertCorrect(createContext("title", "x"));
        assertCorrect(createContext("title", "{x}"));
        assertCorrect(createContext("title", "{x}x{}x{{}}"));
        assertWrong(createContext("title", "{x}x{}}x{{}}"));
        assertWrong(createContext("title", "}"));
        assertWrong(createContext("title", "{"));
    }

    @Test
    public void testAuthorNameChecks() {
        for (String field : InternalBibtexFields.getPersonNameFields()) {
            assertCorrect(createContext(field, ""));
            assertCorrect(createContext(field, "Knuth"));
            assertCorrect(createContext(field, "   Knuth, Donald E. "));
            assertCorrect(createContext(field, "Knuth, Donald E. and Kurt Cobain and A. Einstein"));
            assertCorrect(createContext(field, "Donald E. Knuth and Kurt Cobain and A. Einstein"));
            assertWrong(createContext(field, ", and Kurt Cobain and A. Einstein"));
            assertWrong(createContext(field, "Donald E. Knuth and Kurt Cobain and ,"));
            assertWrong(createContext(field, "and Kurt Cobain and A. Einstein"));
            assertWrong(createContext(field, "Donald E. Knuth and Kurt Cobain and"));
        }
    }

    @Test
    public void testTitleChecks() {
        assertCorrect(withMode(createContext("title", "This is a title"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("title", "This is a Title"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("title", "This is a {T}itle"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("title", "{This is a Title}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("title", "This is a {Title}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("title", "{C}urrent {C}hronicle"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("title", "{A Model-Driven Approach for Monitoring {ebBP} BusinessTransactions}"), BibDatabaseMode.BIBTEX));

        assertCorrect(withMode(createContext("title", "This is a title"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("title", "This is a Title"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("title", "This is a {T}itle"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("title", "{This is a Title}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("title", "This is a {Title}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("title", "{C}urrent {C}hronicle"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("title", "{A Model-Driven Approach for Monitoring {ebBP} BusinessTransactions}"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    public void testAbbreviationChecks() {
        for (String field : Arrays.asList("booktitle", "journal")) {
            assertCorrect(createContext(field, "Proceedings of the"));
            assertWrong(createContext(field, "Proc. of the"));
        }
    }

    @Test
    public void testFileChecks() {
        MetaData metaData = Mockito.mock(MetaData.class);
        Mockito.when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("."));
        Mockito.when(metaData.getUserFileDirectory(any(String.class))).thenReturn(Optional.empty());
        // FIXME: must be set as checkBibtexDatabase only activates title checker based on database mode
        Mockito.when(metaData.getMode()).thenReturn(Optional.of(BibDatabaseMode.BIBTEX));

        assertCorrect(createContext("file", ":build.gradle:gradle", metaData));
        assertCorrect(createContext("file", "description:build.gradle:gradle", metaData));
        assertWrong(createContext("file", ":asflakjfwofja:PDF", metaData));
    }

    @Test
    public void fileCheckFindsFilesRelativeToBibFile() throws IOException {
        File bibFile = testFolder.newFile("lit.bib");
        testFolder.newFile("file.pdf");

        BibDatabaseContext databaseContext = createContext("file", ":file.pdf:PDF");
        databaseContext.setDatabaseFile(bibFile);

        assertCorrect(databaseContext);
    }

    @Test
    public void testTypeChecks() {
        assertCorrect(createContext("pages", "11--15", "inproceedings"));
        assertWrong(createContext("pages", "11--15", "proceedings"));
    }

    @Test
    public void testBooktitleChecks() {
        assertCorrect(createContext("booktitle", "2014 Fourth International Conference on Digital Information and Communication Technology and it's Applications (DICTAP)", "proceedings"));
        assertWrong(createContext("booktitle", "Digital Information and Communication Technology and it's Applications (DICTAP), 2014 Fourth International Conference on", "proceedings"));
    }

    @Test
    public void testPageNumbersChecks() {
        assertCorrect(createContext("pages", "1--2"));
        assertCorrect(createContext("pages", "12"));
        assertWrong(createContext("pages", "1-2"));
        assertCorrect(createContext("pages", "1,2,3"));
        assertCorrect(createContext("pages", "43+"));
        assertWrong(createContext("pages", "1 2"));
        assertWrong(createContext("pages", "{1}-{2}"));
        assertCorrect(createContext("pages", "7,41,73--97"));
        assertCorrect(createContext("pages", "7,41--42,73"));
        assertCorrect(createContext("pages", "7--11,41--43,73"));
        assertCorrect(createContext("pages", "7+,41--43,73"));
    }

    @Test
    public void testBiblatexPageNumbersChecks() {
        assertCorrect(withMode(createContext("pages", "1--2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("pages", "12"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("pages", "1-2"), BibDatabaseMode.BIBLATEX)); // only diff to bibtex
        assertCorrect(withMode(createContext("pages", "1,2,3"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("pages", "43+"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("pages", "1 2"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("pages", "{1}-{2}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("pages", "7,41,73--97"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("pages", "7,41--42,73"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("pages", "7--11,41--43,73"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("pages", "7+,41--43,73"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    public void testBibStringChecks() {
        assertCorrect(createContext("title", "Not a single hash mark"));
        assertCorrect(createContext("month", "#jan#"));
        assertCorrect(createContext("author", "#einstein# and #newton#"));
        assertWrong(createContext("month", "#jan"));
        assertWrong(createContext("author", "#einstein# #amp; #newton#"));
    }

    @Test
    public void testHTMLCharacterChecks() {
        assertCorrect(createContext("title", "Not a single {HTML} character"));
        assertCorrect(createContext("month", "#jan#"));
        assertCorrect(createContext("author", "A. Einstein and I. Newton"));
        assertCorrect(createContext("url", "http://www.thinkmind.org/index.php?view=article&amp;articleid=cloud_computing_2013_1_20_20130"));
        assertWrong(createContext("author", "Lenhard, J&ouml;rg"));
        assertWrong(createContext("author", "Lenhard, J&#227;rg"));
        assertWrong(createContext("journal", "&Auml;rling Str&ouml;m for &#8211; &#x2031;"));
    }

    @Test
    public void testISSNChecks() {
        assertCorrect(createContext("issn", "0020-7217"));
        assertCorrect(createContext("issn", "1687-6180"));
        assertCorrect(createContext("issn", "2434-561x"));
        assertWrong(createContext("issn", "Some other stuff"));
        assertWrong(createContext("issn", "0020-7218"));
    }

    @Test
    public void testISBNChecks() {
        assertCorrect(createContext("isbn", "0-201-53082-1"));
        assertCorrect(createContext("isbn", "0-9752298-0-X"));
        assertCorrect(createContext("isbn", "978-0-306-40615-7"));
        assertWrong(createContext("isbn", "Some other stuff"));
        assertWrong(createContext("isbn", "0-201-53082-2"));
        assertWrong(createContext("isbn", "978-0-306-40615-8"));
    }

    @Test
    public void testASCIIChecks() {
        assertCorrect(createContext("title", "Only ascii characters!'@12"));
        assertWrong(createContext("month", "Umlauts are nöt ällowed"));
        assertWrong(createContext("author", "Some unicode ⊕"));
    }

    private BibDatabaseContext createContext(String field, String value, String type) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        entry.setType(type);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase, new Defaults());
    }

    private BibDatabaseContext createContext(String field, String value, MetaData metaData) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase, metaData, new Defaults());
    }

    private BibDatabaseContext createContext(String field, String value) {
        return createContext(field, value, new MetaData());
    }

    private void assertWrong(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                JabRefPreferences.getInstance().getFileDirectoryPreferences())
                .checkBibtexDatabase();
        assertFalse(messages.toString(), messages.isEmpty());
    }

    private void assertCorrect(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                JabRefPreferences.getInstance().getFileDirectoryPreferences()).checkBibtexDatabase();
        assertEquals(Collections.emptyList(), messages);
    }

    private BibDatabaseContext withMode(BibDatabaseContext context, BibDatabaseMode mode) {
        context.setMode(mode);
        return context;
    }

}
