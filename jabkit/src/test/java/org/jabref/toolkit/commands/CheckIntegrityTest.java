package org.jabref.toolkit.commands;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

class CheckIntegrityTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void redirectStdout() {
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void returns2WhenImportFileReturnsEmpty() {
        CheckIntegrity cmd = new CheckIntegrity();

        JabKit jabKit = mock(JabKit.class);

        setField(cmd, "jabKit", jabKit);
        setField(cmd, "inputFile", Path.of("missing.bib"));

        JabKit.SharedOptions shared = new JabKit.SharedOptions();
        shared.porcelain = true;
        setField(cmd, "sharedOptions", shared);

        try (MockedStatic<JabKit> mocked = mockStatic(JabKit.class)) {
            mocked.when(() -> JabKit.importFile(anyString(), any(), any(), anyBoolean()))
                  .thenReturn(Optional.empty());

            int exitCode = cmd.call();

            assertEquals(2, exitCode);
            assertTrue(outContent.toString().contains("Unable to open file"),
                    "Expected error message for missing input file");
        }
    }

    @Test
    void returns2WhenParserResultIsInvalid() {
        CheckIntegrity cmd = new CheckIntegrity();

        JabKit jabKit = mock(JabKit.class);

        setField(cmd, "jabKit", jabKit);
        setField(cmd, "inputFile", Path.of("invalid.bib"));

        JabKit.SharedOptions shared = new JabKit.SharedOptions();
        shared.porcelain = true;
        setField(cmd, "sharedOptions", shared);

        ParserResult parserResult = mock(ParserResult.class);
        when(parserResult.isInvalid()).thenReturn(true);

        try (MockedStatic<JabKit> mocked = mockStatic(JabKit.class)) {
            mocked.when(() -> JabKit.importFile(anyString(), any(), any(), anyBoolean()))
                  .thenReturn(Optional.of(parserResult));

            int exitCode = cmd.call();

            assertEquals(2, exitCode);
        }
    }

    @Test
    void returns3ForUnknownOutputFormat() {
        CheckIntegrity cmd = new CheckIntegrity();

        JabKit jabKit = mock(JabKit.class);
        setField(cmd, "jabKit", jabKit);

        CliPreferences cliPrefs = mock(CliPreferences.class);
        setField(jabKit, "cliPreferences", cliPrefs);

        Path input = Path.of("ok.bib");
        setField(cmd, "inputFile", input);
        setField(cmd, "outputFormat", "unknown");

        JabKit.SharedOptions shared = new JabKit.SharedOptions();
        shared.porcelain = true;
        setField(cmd, "sharedOptions", shared);

        ParserResult parserResult = mock(ParserResult.class);
        when(parserResult.isInvalid()).thenReturn(false);

        BibDatabaseContext dbContext = mock(BibDatabaseContext.class);
        when(dbContext.getEntries()).thenReturn(Collections.emptyList());
        when(dbContext.getDatabase()).thenReturn(mock(BibDatabase.class));
        when(parserResult.getDatabaseContext()).thenReturn(dbContext);

        try (MockedStatic<JabKit> mockedImport = mockStatic(JabKit.class);
             MockedStatic<JournalAbbreviationLoader> mockedJournals = mockStatic(JournalAbbreviationLoader.class);
             MockedConstruction<IntegrityCheck> mockedIntegrity =
                     mockConstruction(IntegrityCheck.class, (mock, context) -> {
                         when(mock.checkEntry(any())).thenReturn(Collections.emptyList());
                         when(mock.checkDatabase(any())).thenReturn(Collections.emptyList());
                     })) {

            mockedJournals.when(() -> JournalAbbreviationLoader.loadRepository(any()))
                          .thenReturn(null);

            mockedImport.when(() -> JabKit.importFile(eq(input), eq("bibtex"), any(), eq(true)))
                        .thenReturn(Optional.of(parserResult));

            int exitCode = cmd.call();
            assertEquals(3, exitCode);
        }
    }

    @Test
    void returns0ForCsvOutputFormat() {
        CheckIntegrity cmd = new CheckIntegrity();

        // Mock JabKit + required preferences
        JabKit jabKit = mock(JabKit.class);
        setField(cmd, "jabKit", jabKit);

        CliPreferences cliPrefs = mock(CliPreferences.class);
        when(cliPrefs.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(cliPrefs.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class));
        when(cliPrefs.getJournalAbbreviationPreferences()).thenReturn(mock(JournalAbbreviationPreferences.class));
        setField(jabKit, "cliPreferences", cliPrefs);

        Path input = Path.of("ok.bib");
        setField(cmd, "inputFile", input);
        setField(cmd, "outputFormat", "csv");

        JabKit.SharedOptions shared = new JabKit.SharedOptions();
        shared.porcelain = true;
        setField(cmd, "sharedOptions", shared);

        // ParserResult setup
        ParserResult parserResult = mock(ParserResult.class);
        when(parserResult.isInvalid()).thenReturn(false);

        BibDatabaseContext dbContext = mock(BibDatabaseContext.class);
        when(dbContext.getEntries()).thenReturn(Collections.emptyList());
        when(dbContext.getDatabase()).thenReturn(mock(BibDatabase.class));
        when(parserResult.getDatabaseContext()).thenReturn(dbContext);

        try (MockedStatic<JabKit> mockedImport = mockStatic(JabKit.class);
             MockedStatic<JournalAbbreviationLoader> mockedJournals =
                     mockStatic(JournalAbbreviationLoader.class);
             MockedConstruction<IntegrityCheck> mockedIntegrity =
                     mockConstruction(IntegrityCheck.class, (mock, context) -> {
                         when(mock.checkEntry(any())).thenReturn(Collections.emptyList());
                         when(mock.checkDatabase(any())).thenReturn(Collections.emptyList());
                     })) {

            mockedJournals.when(() -> JournalAbbreviationLoader.loadRepository(any()))
                          .thenReturn(null);

            mockedImport.when(() -> JabKit.importFile(eq(input), eq("bibtex"), any(), eq(true)))
                        .thenReturn(Optional.of(parserResult));

            int exitCode = cmd.call();
            assertEquals(0, exitCode);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to set field " + name, e);
        }
    }
}
