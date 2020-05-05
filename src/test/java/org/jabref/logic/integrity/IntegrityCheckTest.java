package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class IntegrityCheckTest {

    @Test
    void bibTexAcceptsStandardEntryType() {
        assertCorrect(withMode(createContext(StandardField.TITLE, "sometitle", StandardEntryType.Article), BibDatabaseMode.BIBTEX));
<<<<<<< HEAD
=======
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
        assertCorrect(withMode(createContext(StandardField.EDITION, "Third, revised and expanded edition"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.EDITION, "Edition 2000"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.EDITION, "2nd"), BibDatabaseMode.BIBLATEX));
        assertWrong(withMode(createContext(StandardField.EDITION, "1"), BibDatabaseMode.BIBTEX));
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
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: This is a sub title 2"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: this is a sub title 2"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.TITLE, "This is a sub title 1: This is A sub title 2"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.TITLE, "This is a sub title 1: this is A sub title 2"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: This is {A} sub title 2"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: this is {A} sub title 2"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1...This is a sub title 2"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.TITLE, "This is a sub title 1... this is a sub Title 2"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is; A sub title 1.... This is a sub title 2"), BibDatabaseMode.BIBTEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This!is!!A!Title??"), BibDatabaseMode.BIBTEX));
        assertWrong(withMode(createContext(StandardField.TITLE, "This!is!!A!TitlE??"), BibDatabaseMode.BIBTEX));

        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a title"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a Title"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a {T}itle"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{This is a Title}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a {Title}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{C}urrent {C}hronicle"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "{A Model-Driven Approach for Monitoring {ebBP} BusinessTransactions}"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: This is a sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: this is a sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: This is A sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: this is A sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: This is {A} sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1: this is {A} sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1...This is a sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is a sub title 1... this is a sub Title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This is; A sub title 1.... This is a sub title 2"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This!is!!A!Title??"), BibDatabaseMode.BIBLATEX));
        assertCorrect(withMode(createContext(StandardField.TITLE, "This!is!!A!TitlE??"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testAbbreviationChecks() {
        for (Field field : Arrays.asList(StandardField.BOOKTITLE, StandardField.JOURNAL)) {
            assertCorrect(createContext(field, "2D Materials"));
            assertCorrect(createContext(field, ""));
            assertWrong(createContext(field, "2D Mater."));
        }
    }

    @Test
    void testJournalIsKnownInAbbreviationList() {
        assertCorrect(createContext(StandardField.JOURNAL, "2D Materials"));
        assertWrong(createContext(StandardField.JOURNAL, "Some unknown journal"));
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
        databaseContext.setDatabasePath(bibFile);

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
>>>>>>> master
    }

    @Test
    void bibTexDoesNotAcceptIEEETranEntryType() {
        assertWrong(withMode(createContext(StandardField.TITLE, "sometitle", IEEETranEntryType.Patent), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsIEEETranEntryType() {
        assertCorrect((withMode(createContext(StandardField.TITLE, "sometitle", IEEETranEntryType.Patent), BibDatabaseMode.BIBLATEX)));
    }

    @Test
    void bibLaTexAcceptsStandardEntryType() {
        assertCorrect(withMode(createContext(StandardField.TITLE, "sometitle", StandardEntryType.Article), BibDatabaseMode.BIBLATEX));
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
                JournalAbbreviationLoader.loadBuiltInRepository(), true, false)
                .checkDatabase();

        assertEquals(clonedEntry, entry);
    }

    protected static BibDatabaseContext createContext(Field field, String value, EntryType type) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        entry.setType(type);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase);
    }

    protected static BibDatabaseContext createContext(Field field, String value, MetaData metaData) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase, metaData);
    }

    protected static BibDatabaseContext createContext(Field field, String value) {
        MetaData metaData = new MetaData();
        metaData.setMode(BibDatabaseMode.BIBTEX);
        return createContext(field, value, metaData);
    }

    protected static void assertWrong(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                JournalAbbreviationLoader.loadBuiltInRepository(), true, false)
                .checkDatabase();
        assertNotEquals(Collections.emptyList(), messages);
    }

    protected static void assertCorrect(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                JournalAbbreviationLoader.loadBuiltInRepository(), true, false
        ).checkDatabase();
        assertEquals(Collections.emptyList(), messages);
    }

    protected static void assertCorrect(BibDatabaseContext context, boolean allowIntegerEdition) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
<<<<<<< HEAD
                                                             mock(FilePreferences.class),
                                                             createBibtexKeyPatternPreferences(),
                                                             new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true,
                                                             allowIntegerEdition).checkDatabase();
=======
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                JournalAbbreviationLoader.loadBuiltInRepository(), true,
                allowIntegerEdition
        ).checkDatabase();
>>>>>>> master
        assertEquals(Collections.emptyList(), messages);
    }

    private static BibtexKeyPatternPreferences createBibtexKeyPatternPreferences() {
        final GlobalBibtexKeyPattern keyPattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");
        return new BibtexKeyPatternPreferences(
                "",
                "",
                false,
                false,
                false,
                keyPattern,
                ',',
                false);
    }

    protected static BibDatabaseContext withMode(BibDatabaseContext context, BibDatabaseMode mode) {
        context.setMode(mode);
        return context;
    }
}