package org.jabref.logic.integrity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.journals.JournalAbbreviationLoader;
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
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class tests the Integrity Checker as a whole.
 * Aspects are: selected fields, issues arising in a complete BibTeX entry, ... When testing a checker works with a certain input,
 * this test has to go to a test belonging to the respective checker. See PersonNamesCheckerTest for an example test.
 */
class IntegrityCheckTest {

    @Test
    void bibTexAcceptsStandardEntryType() {
        assertCorrect(withMode(createContext(StandardField.TITLE, "sometitle", StandardEntryType.Article), BibDatabaseMode.BIBTEX));
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

    @ParameterizedTest
    @MethodSource("provideCorrectFormat")
    void authorNameChecksCorrectFormat(String input) {
        for (Field field : FieldFactory.getPersonNameFields()) {
            assertCorrect(withMode(createContext(field, input), BibDatabaseMode.BIBLATEX));
        }
    }

    @ParameterizedTest
    @MethodSource("provideIncorrectFormat")
    void authorNameChecksIncorrectFormat(String input) {
        for (Field field : FieldFactory.getPersonNameFields()) {
            assertWrong(withMode(createContext(field, input), BibDatabaseMode.BIBLATEX));
        }
    }

    private static Stream<String> provideCorrectFormat() {
        return Stream.of("", "Knuth", "Donald E. Knuth and Kurt Cobain and A. Einstein");
    }

    private static Stream<String> provideIncorrectFormat() {
        return Stream.of("   Knuth, Donald E. ",
                "Knuth, Donald E. and Kurt Cobain and A. Einstein",
                ", and Kurt Cobain and A. Einstein", "Donald E. Knuth and Kurt Cobain and ,",
                "and Kurt Cobain and A. Einstein", "Donald E. Knuth and Kurt Cobain and");
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
                createCitationKeyPatternPreferences(),
                JournalAbbreviationLoader.loadBuiltInRepository(), false)
                .check();

        assertEquals(clonedEntry, entry);
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
                createCitationKeyPatternPreferences(),
                JournalAbbreviationLoader.loadBuiltInRepository(), false)
                .check();
        assertNotEquals(Collections.emptyList(), messages);
    }

    private void assertCorrect(BibDatabaseContext context) {
        FilePreferences filePreferencesMock = mock(FilePreferences.class);
        when(filePreferencesMock.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                filePreferencesMock,
                createCitationKeyPatternPreferences(),
                JournalAbbreviationLoader.loadBuiltInRepository(), false
        ).check();
        assertEquals(Collections.emptyList(), messages);
    }

    private void assertCorrect(BibDatabaseContext context, boolean allowIntegerEdition) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createCitationKeyPatternPreferences(),
                JournalAbbreviationLoader.loadBuiltInRepository(),
                allowIntegerEdition).check();
        assertEquals(Collections.emptyList(), messages);
    }

    private CitationKeyPatternPreferences createCitationKeyPatternPreferences() {
        final GlobalCitationKeyPattern keyPattern = GlobalCitationKeyPattern.fromPattern("[auth][year]");
        return new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B,
                "",
                "",
                CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS,
                keyPattern,
                ',');
    }

    private BibDatabaseContext withMode(BibDatabaseContext context, BibDatabaseMode mode) {
        context.setMode(mode);
        return context;
    }
}
