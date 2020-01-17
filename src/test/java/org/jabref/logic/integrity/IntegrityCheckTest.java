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
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

class IntegrityCheckTest {

    @Test
    void testEntryTypeChecks() {
        assertCorrect(withMode(createContext(StandardField.TITLE, "sometitle", StandardEntryType.Article), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.TITLE, "sometitle", IEEETranEntryType.Patent), BibDatabaseMode.BIBTEX));
        assertCorrect((withMode(createContext(StandardField.TITLE, "sometitle", IEEETranEntryType.Patent), BibDatabaseMode.BIBLATEX)));
        assertCorrect(withMode(createContext(StandardField.TITLE, "sometitle", StandardEntryType.Article), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testUrlChecks() {
        assertCorrect(createContext(StandardField.URL, "http://www.google.com"));
        assertCorrect(createContext(StandardField.URL, "file://c:/asdf/asdf"));
        assertCorrect(createContext(StandardField.URL, "http://scikit-learn.org/stable/modules/ensemble.html#random-forests"));

        assertWrong(createContext(StandardField.URL, "www.google.com"));
        assertWrong(createContext(StandardField.URL, "google.com"));
        assertWrong(createContext(StandardField.URL, "c:/asdf/asdf"));
    }

    @Test
    void testYearChecks() {
        assertCorrect(createContext(StandardField.YEAR, "2014"));
        assertCorrect(createContext(StandardField.YEAR, "1986"));
        assertCorrect(createContext(StandardField.YEAR, "around 1986"));
        assertCorrect(createContext(StandardField.YEAR, "(around 1986)"));
        assertCorrect(createContext(StandardField.YEAR, "1986,"));
        assertCorrect(createContext(StandardField.YEAR, "1986}%"));
        assertCorrect(createContext(StandardField.YEAR, "1986(){},.;!?<>%&$"));
        assertWrong(createContext(StandardField.YEAR, "abc"));
        assertWrong(createContext(StandardField.YEAR, "86"));
        assertWrong(createContext(StandardField.YEAR, "204"));
        assertWrong(createContext(StandardField.YEAR, "1986a"));
        assertWrong(createContext(StandardField.YEAR, "(1986a)"));
        assertWrong(createContext(StandardField.YEAR, "1986a,"));
        assertWrong(createContext(StandardField.YEAR, "1986}a%"));
        assertWrong(createContext(StandardField.YEAR, "1986a(){},.;!?<>%&$"));
    }

    @Test
    void testEditionChecks() {
        assertCorrect(withMode(createContext(StandardField.EDITION, "Second"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.EDITION, "Third"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.EDITION, "second"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.EDITION, "2"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.EDITION, "2"), BibDatabaseMode.BIBTEX), true);
        assertWrong(withMode(createContext(StandardField.EDITION, "2nd"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.EDITION, "2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.EDITION, "10"), BibDatabaseMode.BIBLATEX));
        assertCorrect(
                withMode(createContext(StandardField.EDITION, "Third, revised and expanded edition"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.EDITION, "Edition 2000"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.EDITION, "2nd"), BibDatabaseMode.BIBLATEX));
        assertWrong(createContext(StandardField.EDITION, "1"));
    }

    @Test
    void testNoteChecks() {
        assertCorrect(withMode(createContext(StandardField.NOTE, "Lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.NOTE, "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.NOTE, "lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.NOTE, "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.NOTE, "\\url{someurl}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.NOTE, "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testHowpublishedChecks() {
        assertCorrect(withMode(createContext(StandardField.HOWPUBLISHED, "Lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.HOWPUBLISHED, "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.HOWPUBLISHED, "lorem ipsum"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.HOWPUBLISHED, "\\url{someurl}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.HOWPUBLISHED, "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.HOWPUBLISHED, "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testMonthChecks() {
        assertCorrect(withMode(createContext(StandardField.MONTH, "#mar#"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.MONTH, "#dec#"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "#bla#"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "Dec"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "December"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "Lorem"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "10"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.MONTH, "1"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.MONTH, "10"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.MONTH, "#jan#"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "jan"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "january"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "January"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.MONTH, "Lorem"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testJournaltitleChecks() {
        assertWrong(withMode(createContext(StandardField.JOURNALTITLE, "A journal"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.JOURNAL, "A journal"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void testBibtexkeyChecks() {
        final BibDatabaseContext correctContext = createContext(InternalField.KEY_FIELD, "Knuth2014");
        correctContext.getDatabase().getEntries().get(0).setField(StandardField.AUTHOR, "Knuth");
        correctContext.getDatabase().getEntries().get(0).setField(StandardField.YEAR, "2014");
        assertCorrect(correctContext);

        final BibDatabaseContext wrongContext = createContext(InternalField.KEY_FIELD, "Knuth2014a");
        wrongContext.getDatabase().getEntries().get(0).setField(StandardField.AUTHOR, "Knuth");
        wrongContext.getDatabase().getEntries().get(0).setField(StandardField.YEAR, "2014");
        assertWrong(wrongContext);
    }

    @Test
    void testBracketChecks() {
        assertCorrect(createContext(StandardField.TITLE, "x"));
        assertCorrect(createContext(StandardField.TITLE, "{x}"));
        assertCorrect(createContext(StandardField.TITLE, "{x}x{}x{{}}"));
        assertWrong(createContext(StandardField.TITLE, "{x}x{}}x{{}}"));
        assertWrong(createContext(StandardField.TITLE, "}"));
        assertWrong(createContext(StandardField.TITLE, "{"));
    }

    @Test
    void testAuthorNameChecks() {
        for (Field field : FieldFactory.getPersonNameFields()) {
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
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a title"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.TITLE, "This is a Title"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a {T}itle"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{This is a Title}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a {Title}"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{C}urrent {C}hronicle"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{A Model-Driven Approach for Monitoring {ebBP} BusinessTransactions}"), BibDatabaseMode.BIBTEX));

        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a title"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a Title"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a {T}itle"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{This is a Title}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a {Title}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{C}urrent {C}hronicle"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{A Model-Driven Approach for Monitoring {ebBP} BusinessTransactions}"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testAbbreviationChecks() {
        for (Field field : Arrays.asList(StandardField.BOOKTITLE, StandardField.JOURNAL)) {
            assertCorrect(createContext(field, "IEEE Software"));
            assertCorrect(createContext(field, ""));
            assertWrong(createContext(field, "IEEE SW"));
        }
    }

    @Test
    void testJournalIsKnownInAbbreviationList() {
        assertCorrect(createContext(StandardField.JOURNAL, "IEEE Software"));
        assertWrong(createContext(StandardField.JOURNAL, "IEEE Whocares"));
    }

    @Test
    void testFileChecks() {
        MetaData metaData = mock(MetaData.class);
        Mockito.when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("."));
        Mockito.when(metaData.getUserFileDirectory(any(String.class))).thenReturn(Optional.empty());
        // FIXME: must be set as checkBibtexDatabase only activates title checker based on database mode
        Mockito.when(metaData.getMode()).thenReturn(Optional.of(BibDatabaseMode.BIBTEX));

        assertCorrect(createContext(StandardField.FILE, ":build.gradle:gradle", metaData));
        assertCorrect(createContext(StandardField.FILE, "description:build.gradle:gradle", metaData));
        assertWrong(createContext(StandardField.FILE, ":asflakjfwofja:PDF", metaData));
    }

    @Test
    void fileCheckFindsFilesRelativeToBibFile(@TempDir Path testFolder) throws IOException {
        Path bibFile = testFolder.resolve("lit.bib");
        Files.createFile(bibFile);
        Path pdfFile = testFolder.resolve("file.pdf");
        Files.createFile(pdfFile);

        BibDatabaseContext databaseContext = createContext(StandardField.FILE, ":file.pdf:PDF");
        databaseContext.setDatabaseFile(bibFile);

        assertCorrect(databaseContext);
    }

    @Test
    void testTypeChecks() {
        assertCorrect(createContext(StandardField.PAGES, "11--15", StandardEntryType.InProceedings));
        assertWrong(createContext(StandardField.PAGES, "11--15", StandardEntryType.Proceedings));
    }

    @Test
    void testBooktitleChecks() {
        assertCorrect(createContext(StandardField.BOOKTITLE, "2014 Fourth International Conference on Digital Information and Communication Technology and it's Applications (DICTAP)", StandardEntryType.Proceedings));
        assertWrong(createContext(StandardField.BOOKTITLE, "Digital Information and Communication Technology and it's Applications (DICTAP), 2014 Fourth International Conference on", StandardEntryType.Proceedings));
    }

    @Test
    void testPageNumbersChecks() {
        assertCorrect(createContext(StandardField.PAGES, "1--2"));
        assertCorrect(createContext(StandardField.PAGES, "12"));
        assertWrong(createContext(StandardField.PAGES, "1-2"));
        assertCorrect(createContext(StandardField.PAGES, "1,2,3"));
        assertCorrect(createContext(StandardField.PAGES, "43+"));
        assertWrong(createContext(StandardField.PAGES, "1 2"));
        assertWrong(createContext(StandardField.PAGES, "{1}-{2}"));
        assertCorrect(createContext(StandardField.PAGES, "7,41,73--97"));
        assertCorrect(createContext(StandardField.PAGES, "7,41--42,73"));
        assertCorrect(createContext(StandardField.PAGES, "7--11,41--43,73"));
        assertCorrect(createContext(StandardField.PAGES, "7+,41--43,73"));
    }

    @Test
    void testBiblatexPageNumbersChecks() {
        assertCorrect(withMode(createContext(StandardField.PAGES, "1--2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.PAGES, "12"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.PAGES, "1-2"), BibDatabaseMode.BIBLATEX)); // only diff to bibtex
        assertCorrect(withMode(createContext(StandardField.PAGES, "1,2,3"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.PAGES, "43+"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.PAGES, "1 2"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.PAGES, "{1}-{2}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.PAGES, "7,41,73--97"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.PAGES, "7,41--42,73"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.PAGES, "7--11,41--43,73"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.PAGES, "7+,41--43,73"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testBibStringChecks() {
        assertCorrect(createContext(StandardField.TITLE, "Not a single hash mark"));
        assertCorrect(createContext(StandardField.MONTH, "#jan#"));
        assertCorrect(createContext(StandardField.AUTHOR, "#einstein# and #newton#"));
        assertWrong(createContext(StandardField.MONTH, "#jan"));
        assertWrong(createContext(StandardField.AUTHOR, "#einstein# #amp; #newton#"));
    }

    @Test
    void testHTMLCharacterChecks() {
        assertCorrect(createContext(StandardField.TITLE, "Not a single {HTML} character"));
        assertCorrect(createContext(StandardField.MONTH, "#jan#"));
        assertCorrect(createContext(StandardField.AUTHOR, "A. Einstein and I. Newton"));
        assertCorrect(createContext(StandardField.URL, "http://www.thinkmind.org/index.php?view=article&amp;articleid=cloud_computing_2013_1_20_20130"));
        assertWrong(createContext(StandardField.AUTHOR, "Lenhard, J&ouml;rg"));
        assertWrong(createContext(StandardField.AUTHOR, "Lenhard, J&#227;rg"));
        assertWrong(createContext(StandardField.JOURNAL, "&Auml;rling Str&ouml;m for &#8211; &#x2031;"));
    }

    @Test
    void testISSNChecks() {
        assertCorrect(createContext(StandardField.ISSN, "0020-7217"));
        assertCorrect(createContext(StandardField.ISSN, "1687-6180"));
        assertCorrect(createContext(StandardField.ISSN, "2434-561x"));
        assertWrong(createContext(StandardField.ISSN, "Some other stuff"));
        assertWrong(createContext(StandardField.ISSN, "0020-7218"));
    }

    @Test
    void testISBNChecks() {
        assertCorrect(createContext(StandardField.ISBN, "0-201-53082-1"));
        assertCorrect(createContext(StandardField.ISBN, "0-9752298-0-X"));
        assertCorrect(createContext(StandardField.ISBN, "978-0-306-40615-7"));
        assertWrong(createContext(StandardField.ISBN, "Some other stuff"));
        assertWrong(createContext(StandardField.ISBN, "0-201-53082-2"));
        assertWrong(createContext(StandardField.ISBN, "978-0-306-40615-8"));
    }

    @Test
    void testDOIChecks() {
        assertCorrect(createContext(StandardField.DOI, "10.1023/A:1022883727209"));
        assertCorrect(createContext(StandardField.DOI, "10.17487/rfc1436"));
        assertCorrect(createContext(StandardField.DOI, "10.1002/(SICI)1097-4571(199205)43:4<284::AID-ASI3>3.0.CO;2-0"));
        assertWrong(createContext(StandardField.DOI, "asdf"));
    }

    @Test
    void testEntryIsUnchangedAfterChecks() {
        BibEntry entry = new BibEntry();

        // populate with all known fields
        for (Field field : FieldFactory.getCommonFields()) {
            entry.setField(field, UUID.randomUUID().toString());
        }
        // add a random field
        entry.setField(StandardField.EPRINT, UUID.randomUUID().toString());

        // duplicate entry
        BibEntry clonedEntry = (BibEntry) entry.clone();

        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);

        new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                           new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true, false)
                .checkDatabase();

        assertEquals(clonedEntry, entry);
    }

    @Test
    void testASCIIChecks() {
        assertCorrect(createContext(StandardField.TITLE, "Only ascii characters!'@12"));
        assertWrong(createContext(StandardField.MONTH, "Umlauts are nöt ällowed"));
        assertWrong(createContext(StandardField.AUTHOR, "Some unicode ⊕"));
    }

    private BibDatabaseContext createContext(Field field, String value, EntryType type) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        entry.setType(type);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase);
    }

    private BibDatabaseContext createContext(Field field, String value, MetaData metaData) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase, metaData);
    }

    private BibDatabaseContext createContext(Field field, String value) {
        MetaData metaData = new MetaData();
        metaData.setMode(BibDatabaseMode.BIBTEX);
        return createContext(field, value, metaData);
    }

    private void assertWrong(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true, false)
                .checkDatabase();
        assertNotEquals(Collections.emptyList(), messages);
    }

    private void assertCorrect(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true, false
        ).checkDatabase();
        assertEquals(Collections.emptyList(), messages);
    }

    private void assertCorrect(BibDatabaseContext context, boolean allowIntegerEdition) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true,
                allowIntegerEdition
        ).checkDatabase();
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
