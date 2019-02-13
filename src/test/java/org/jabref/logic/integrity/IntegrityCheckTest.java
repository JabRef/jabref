package org.jabref.logic.integrity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.Defaults;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

class IntegrityCheckTest {

    @Test
    void testEntryTypeChecks() {
        assertCorrect(withMode(createContext("title", "sometitle", "article"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("title", "sometitle", "patent"), BibDatabaseMode.BIBTEX));
        assertCorrect((withMode(createContext("title", "sometitle", "patent"), BibDatabaseMode.BIBLATEX)));
        assertCorrect(withMode(createContext("title", "sometitle", "article"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testUrlChecks() {
        assertCorrect(createContext("url", "http://www.google.com"));
        assertCorrect(createContext("url", "file://c:/asdf/asdf"));
        assertCorrect(createContext("url", "http://scikit-learn.org/stable/modules/ensemble.html#random-forests"));

        assertWrong(createContext("url", "www.google.com"));
        assertWrong(createContext("url", "google.com"));
        assertWrong(createContext("url", "c:/asdf/asdf"));
    }

    @Test
    void testYearChecks() {
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
    void testEditionChecks() {
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
        assertWrong(createContext("edition", "1"));
    }

    @Test
    void testNoteChecks() {
        assertCorrect(withMode(createContext("note", "Lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("note", "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("note", "lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("note", "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("note", "\\url{someurl}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("note", "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testHowpublishedChecks() {
        assertCorrect(withMode(createContext("howpublished", "Lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("howpublished", "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext("howpublished", "lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("howpublished", "\\url{someurl}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext("howpublished", "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext("howpublished", "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testMonthChecks() {
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
    void testJournaltitleChecks() {
        assertWrong(withMode(createContext("journaltitle", "A journal"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext("journaltitle", "A journal"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void testBibtexkeyChecks() {
        final BibDatabaseContext correctContext = createContext("bibtexkey", "Knuth2014");
        correctContext.getDatabase().getEntries().get(0).setField("author", "Knuth");
        correctContext.getDatabase().getEntries().get(0).setField("year", "2014");
        assertCorrect(correctContext);

        final BibDatabaseContext wrongContext = createContext("bibtexkey", "Knuth2014a");
        wrongContext.getDatabase().getEntries().get(0).setField("author", "Knuth");
        wrongContext.getDatabase().getEntries().get(0).setField("year", "2014");
        assertWrong(wrongContext);
    }

    @Test
    void testBracketChecks() {
        assertCorrect(createContext("title", "x"));
        assertCorrect(createContext("title", "{x}"));
        assertCorrect(createContext("title", "{x}x{}x{{}}"));
        assertWrong(createContext("title", "{x}x{}}x{{}}"));
        assertWrong(createContext("title", "}"));
        assertWrong(createContext("title", "{"));
    }

    @Test
    void testAuthorNameChecks() {
        for (String field : InternalBibtexFields.getPersonNameFields()) {
            // getPersonNameFields returns fields that are available in biblatex only
            // if run without mode, the NoBibtexFieldChecker will complain that "afterword" is a biblatex only field
            assertCorrect(withMode(createContext(field, ""), BibDatabaseMode.BIBLATEX));
            assertCorrect(withMode(createContext(field, "Knuth"), BibDatabaseMode.BIBLATEX));
            assertWrong(withMode(createContext(field, "   Knuth, Donald E. "), BibDatabaseMode.BIBLATEX));
            assertWrong(withMode(createContext(field, "Knuth, Donald E. and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
            assertCorrect(withMode(createContext(field, "Donald E. Knuth and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
            assertWrong(withMode(createContext(field, ", and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
            assertWrong(withMode(createContext(field, "Donald E. Knuth and Kurt Cobain and ,"), BibDatabaseMode.BIBLATEX));
            assertWrong(withMode(createContext(field, "and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
            assertWrong(withMode(createContext(field, "Donald E. Knuth and Kurt Cobain and"), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void testTitleChecks() {
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
    void testAbbreviationChecks() {
        for (String field : Arrays.asList("booktitle", "journal")) {
            assertCorrect(createContext(field, "IEEE Software"));
            assertCorrect(createContext(field, ""));
            assertWrong(createContext(field, "IEEE SW"));
        }
    }

    @Test
    void testJournalIsKnownInAbbreviationList() {
        assertCorrect(createContext("journal", "IEEE Software"));
        assertWrong(createContext("journal", "IEEE Whocares"));
    }

    @Test
    void testFileChecks() {
        MetaData metaData = mock(MetaData.class);
        Mockito.when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("."));
        Mockito.when(metaData.getUserFileDirectory(any(String.class))).thenReturn(Optional.empty());
        // FIXME: must be set as checkBibtexDatabase only activates title checker based on database mode
        Mockito.when(metaData.getMode()).thenReturn(Optional.of(BibDatabaseMode.BIBTEX));

        assertCorrect(createContext("file", ":build.gradle:gradle", metaData));
        assertCorrect(createContext("file", "description:build.gradle:gradle", metaData));
        assertWrong(createContext("file", ":asflakjfwofja:PDF", metaData));
    }

    @Test
    void fileCheckFindsFilesRelativeToBibFile(@TempDir Path testFolder) throws IOException {
        Path bibFile = testFolder.resolve("lit.bib");
        Files.createFile(bibFile);
        Path pdfFile = testFolder.resolve("file.pdf");
        Files.createFile(pdfFile);

        BibDatabaseContext databaseContext = createContext("file", ":file.pdf:PDF");
        databaseContext.setDatabaseFile(bibFile);

        assertCorrect(databaseContext);
    }

    @Test
    void testTypeChecks() {
        assertCorrect(createContext("pages", "11--15", "inproceedings"));
        assertWrong(createContext("pages", "11--15", "proceedings"));
    }

    @Test
    void testBooktitleChecks() {
        assertCorrect(createContext("booktitle", "2014 Fourth International Conference on Digital Information and Communication Technology and it's Applications (DICTAP)", "proceedings"));
        assertWrong(createContext("booktitle", "Digital Information and Communication Technology and it's Applications (DICTAP), 2014 Fourth International Conference on", "proceedings"));
    }

    @Test
    void testPageNumbersChecks() {
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
    void testBiblatexPageNumbersChecks() {
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
    void testBibStringChecks() {
        assertCorrect(createContext("title", "Not a single hash mark"));
        assertCorrect(createContext("month", "#jan#"));
        assertCorrect(createContext("author", "#einstein# and #newton#"));
        assertWrong(createContext("month", "#jan"));
        assertWrong(createContext("author", "#einstein# #amp; #newton#"));
    }

    @Test
    void testHTMLCharacterChecks() {
        assertCorrect(createContext("title", "Not a single {HTML} character"));
        assertCorrect(createContext("month", "#jan#"));
        assertCorrect(createContext("author", "A. Einstein and I. Newton"));
        assertCorrect(createContext("url", "http://www.thinkmind.org/index.php?view=article&amp;articleid=cloud_computing_2013_1_20_20130"));
        assertWrong(createContext("author", "Lenhard, J&ouml;rg"));
        assertWrong(createContext("author", "Lenhard, J&#227;rg"));
        assertWrong(createContext("journal", "&Auml;rling Str&ouml;m for &#8211; &#x2031;"));
    }

    @Test
    void testISSNChecks() {
        assertCorrect(createContext("issn", "0020-7217"));
        assertCorrect(createContext("issn", "1687-6180"));
        assertCorrect(createContext("issn", "2434-561x"));
        assertWrong(createContext("issn", "Some other stuff"));
        assertWrong(createContext("issn", "0020-7218"));
    }

    @Test
    void testISBNChecks() {
        assertCorrect(createContext("isbn", "0-201-53082-1"));
        assertCorrect(createContext("isbn", "0-9752298-0-X"));
        assertCorrect(createContext("isbn", "978-0-306-40615-7"));
        assertWrong(createContext("isbn", "Some other stuff"));
        assertWrong(createContext("isbn", "0-201-53082-2"));
        assertWrong(createContext("isbn", "978-0-306-40615-8"));
    }

    @Test
    void testDOIChecks() {
        assertCorrect(createContext("doi", "10.1023/A:1022883727209"));
        assertCorrect(createContext("doi", "10.17487/rfc1436"));
        assertCorrect(createContext("doi", "10.1002/(SICI)1097-4571(199205)43:4<284::AID-ASI3>3.0.CO;2-0"));
        assertWrong(createContext("doi", "asdf"));
    }

    @Test
    void testEntryIsUnchangedAfterChecks() {
        BibEntry entry = new BibEntry();

        // populate with all known fields
        for (String fieldName : InternalBibtexFields.getAllPublicAndInternalFieldNames()) {
            entry.setField(fieldName, UUID.randomUUID().toString());
        }
        // add a random field
        entry.setField(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // duplicate entry
        BibEntry clonedEntry = (BibEntry) entry.clone();

        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase, new Defaults());

        new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true)
                .checkBibtexDatabase();

        assertEquals(clonedEntry, entry);
    }

    @Test
    void testASCIIChecks() {
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
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true)
                .checkBibtexDatabase();
        assertFalse(messages.isEmpty(), messages.toString());
    }

    private void assertCorrect(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true
        ).checkBibtexDatabase();
        assertEquals(Collections.emptyList(), messages);
    }

    private BibtexKeyPatternPreferences createBibtexKeyPatternPreferences() {
        final GlobalBibtexKeyPattern keyPattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");
        return new BibtexKeyPatternPreferences(
                "",
                "",
                false,
                false,
                false,
                keyPattern,
                ',');
    }

    private BibDatabaseContext withMode(BibDatabaseContext context, BibDatabaseMode mode) {
        context.setMode(mode);
        return context;
    }
}
