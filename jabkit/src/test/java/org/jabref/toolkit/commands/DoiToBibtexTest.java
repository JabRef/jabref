package org.jabref.toolkit.commands;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class DoiToBibtexTest {

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void redirectStdoutStderr() {
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStdoutStderr() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void invalidDoiIsSkippedAndOutputEntriesCalledWithEmptyList() throws FetcherException {
        DoiToBibtex cmd = new DoiToBibtex();

        JabKit jabKit = mock(JabKit.class);
        CliPreferences cliPrefs = mock(CliPreferences.class);
        setField(jabKit, "cliPreferences", cliPrefs);
        setField(cmd, "argumentProcessor", jabKit);
        setField(cmd, "dois", new String[]{"not-a-doi"});

        try (MockedStatic<JabKit> mockedOutput = mockStatic(JabKit.class);
             MockedConstruction<CrossRef> mockedCrossRef = mockConstruction(CrossRef.class)) {

            mockedOutput.when(() -> JabKit.outputEntries(any(), anyList())).thenReturn(0);

            int exitCode = cmd.call();

            assertEquals(0, exitCode);
            assertTrue(outContent.toString().contains("is invalid"),
                    "Should report invalid DOI to stdout");

            // No fetch should be attempted for invalid DOI
            assertEquals(1, mockedCrossRef.constructed().size());
            verify(mockedCrossRef.constructed().getFirst(), never()).performSearchById(any());

            mockedOutput.verify(() -> JabKit.outputEntries(eq(cliPrefs), argThat(List::isEmpty)));
        }
    }

    @Test
    void fetcherExceptionIsHandledAndEntryNotAdded() {
        DoiToBibtex cmd = new DoiToBibtex();

        JabKit jabKit = mock(JabKit.class);
        CliPreferences cliPrefs = mock(CliPreferences.class);
        setField(jabKit, "cliPreferences", cliPrefs);
        setField(cmd, "argumentProcessor", jabKit);

        // Use a valid-looking DOI string so DOI. parse succeeds
        String doi = "10.1000/182";
        setField(cmd, "dois", new String[]{doi});

        try (MockedStatic<JabKit> mockedOutput = mockStatic(JabKit.class);
             MockedConstruction<CrossRef> mockedCrossRef =
                     mockConstruction(CrossRef.class, (mock, context) -> {
                         when(mock.performSearchById(any()))
                                 .thenThrow(new FetcherException("nope"));
                     })) {

            mockedOutput.when(() -> JabKit.outputEntries(any(), anyList())).thenReturn(0);

            int exitCode = cmd.call();

            assertEquals(0, exitCode);
            assertTrue(errContent.toString().contains("No data was found for the identifier"),
                    "Should print fetch error to stderr");
            assertTrue(errContent.toString().contains(doi),
                    "Should include DOI in stderr output");

            mockedOutput.verify(() -> JabKit.outputEntries(eq(cliPrefs), argThat(List::isEmpty)));
        }
    }

    @Test
    void successfulFetchAddsEntryAndPassesItToOutputEntries() {
        DoiToBibtex cmd = new DoiToBibtex();

        JabKit jabKit = mock(JabKit.class);
        CliPreferences cliPrefs = mock(CliPreferences.class);
        setField(jabKit, "cliPreferences", cliPrefs);
        setField(cmd, "argumentProcessor", jabKit);

        String doi = "10.1000/182";
        setField(cmd, "dois", new String[]{doi});

        BibEntry fetched = new BibEntry();

        try (MockedStatic<JabKit> mockedOutput = mockStatic(JabKit.class);
             MockedConstruction<CrossRef> mockedCrossRef =
                     mockConstruction(CrossRef.class, (mock, context) -> {
                         when(mock.performSearchById(any())).thenReturn(Optional.of(fetched));
                     })) {

            mockedOutput.when(() -> JabKit.outputEntries(any(), anyList())).thenReturn(7);

            int exitCode = cmd.call();

            assertEquals(7, exitCode);
            mockedOutput.verify(() -> JabKit.outputEntries(eq(cliPrefs), argThat(list -> list.size() == 1 && list.contains(fetched))));
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
