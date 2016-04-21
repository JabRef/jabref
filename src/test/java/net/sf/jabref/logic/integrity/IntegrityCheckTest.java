package net.sf.jabref.logic.integrity;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
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

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testUrlChecks() {
        assertCorrect(createContext("url", "http://www.google.com"));
        assertCorrect(createContext("url", "file://c:/asdf/asdf"));

        assertWrong(createContext("url", "www.google.com"));
        assertWrong(createContext("url", "google.com"));
        assertWrong(createContext("url", "c:/asdf/asdf"));
    }

    @Test
    public void testYearChecks() {
        assertCorrect(createContext("year", "2014"));
        assertCorrect(createContext("year", "1986"));
        assertWrong(createContext("year", "abc"));
        assertWrong(createContext("year", "86"));
        assertWrong(createContext("year", "204"));
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
        for (String field : InternalBibtexFields.BIBLATEX_PERSON_NAME_FIELDS) {
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
        assertWrong(createContext("author", "Lenhard, J&ouml;rg"));
        assertWrong(createContext("author", "Lenhard, J&#227;rg"));
        assertWrong(createContext("journal", "&Auml;rling Str&ouml;m for &#8211; &#x2031;"));
    }

    private BibDatabaseContext createContext(String field, String value, String type) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        entry.setType(type);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase, new Defaults());
    }

    public BibDatabaseContext createContext(String field, String value, MetaData metaData) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase, metaData, new Defaults());
    }

    public BibDatabaseContext createContext(String field, String value) {
        return createContext(field, value, new MetaData());
    }

    private void assertWrong(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context).checkBibtexDatabase();
        assertFalse(messages.toString(), messages.isEmpty());
    }

    private void assertCorrect(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context).checkBibtexDatabase();
        assertEquals(Collections.emptyList(), messages);
    }

    private BibDatabaseContext withMode(BibDatabaseContext context, BibDatabaseMode mode) {
        context.setMode(mode);
        return context;
    }

}
