package org.jabref.gui.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.CleanupTabSelection;
import org.jabref.logic.conferences.ConferenceAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
@ResourceLock("Localization.lang")
class ConferenceCleanupDialogViewModelTest {
    @TempDir
    Path directory;

    // [utest->req~abbreviations.conference-cleanup-ui~1]
    @ParameterizedTest
    @CsvSource(textBlock = """
            ABBREVIATE_DEFAULT,         International Conference on Business Process Management, BPM
            ABBREVIATE_DOTLESS,         International Conference on Business Process Management, BPM
            ABBREVIATE_SHORTEST_UNIQUE, International Conference on Business Process Management, BPM
            UNABBREVIATE,              BPM, International Conference on Business Process Management
            """)
    void cleanupUsesConfiguredConferenceCsvAndSupportsUndo(CleanupPreferences.CleanupStep step, String input, String expected) throws IOException {
        Path csv = directory.resolve("conferences.csv");
        Files.writeString(csv, "International Conference on Business Process Management,BPM");
        CliPreferences preferences = mock(CliPreferences.class);
        when(preferences.getConferenceAbbreviationPreferences()).thenReturn(new ConferenceAbbreviationPreferences(List.of(csv.toString())));
        when(preferences.getJournalAbbreviationPreferences()).thenReturn(JournalAbbreviationPreferences.getDefault());
        when(preferences.getCleanupPreferences()).thenReturn(CleanupPreferences.getDefault());
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferences.getTimestampPreferences()).thenReturn(mock(TimestampPreferences.class));
        BibDatabaseContext context = new BibDatabaseContext();
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, input);
        context.getDatabase().insertEntry(entry);
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(context));
        JabRefUndoManager undoManager = new JabRefUndoManager();
        CleanupDialogViewModel viewModel = new CleanupDialogViewModel(context, preferences, mock(DialogService.class), stateManager, undoManager,
                () -> mock(LibraryTab.class), new CurrentThreadTaskExecutor(), new JournalAbbreviationRepository());
        viewModel.setTargetEntries(List.of(entry));

        JavaFxExtension.invokeAndWait(() -> viewModel.apply(CleanupTabSelection.ofJobs(EnumSet.of(step), EnumSet.of(step)), false));

        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, expected), entry);
        undoManager.undo();
        assertEquals(new BibEntry().withField(StandardField.BOOKTITLE, input), entry);
    }
}
