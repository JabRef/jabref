package org.jabref.gui.citationkeypattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
class GenerateCitationKeyActionTest {

    private final DialogService dialogService = mock(DialogService.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final CliPreferences preferences = mock(CliPreferences.class);

    private BibEntry remainingWang;

    /// "王" and "汪" both transliterate to "Wang". After "王" is removed, the base key "Wang2020" is free again,
    /// so regenerating "汪" would move it from "Wang2020a" to "Wang2020".
    @BeforeEach
    void setUp() {
        BibEntry removedWang = new BibEntry()
                .withField(StandardField.AUTHOR, "王, 小明")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Wang2020");
        remainingWang = new BibEntry()
                .withField(StandardField.AUTHOR, "汪, 小明")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Wang2020a");
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(removedWang, remainingWang)));
        databaseContext.getDatabase().removeEntry(removedWang);

        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(remainingWang));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.getUndoManager(any())).thenReturn(new HeadlessGuiUndoManager());
        when(dialogService.showConfirmationDialogWithOptOutAndWait(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);
    }

    private void useKeyPreferences(boolean avoidOverwrite) {
        GlobalCitationKeyPatterns keyPatterns = GlobalCitationKeyPatterns.fromPattern("[auth:transliterate:camel][year]");
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(new CitationKeyPatternPreferences(
                false,
                avoidOverwrite,
                true,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                CitationKeyPatternPreferences.DEFAULT_UNWANTED_CHARACTERS,
                keyPatterns,
                new SimpleObjectProperty<>(',')));
    }

    private void generateKeysForSelectedEntries() {
        JavaFxExtension.invokeAndWait(() ->
                new GenerateCitationKeyAction(() -> null, dialogService, stateManager, new CurrentThreadTaskExecutor(), preferences).execute());
        JavaFxExtension.awaitEvents();
    }

    /// Records current behaviour, not intended behaviour: confirming the overwrite renames the key, so a citation of "Wang2020a" breaks.
    /// A silently changing key is considered a bug (see https://github.com/JabRef/jabref/issues/9605).
    /// This is the contrast to the test with overwriting avoided, which keeps the key.
    @Test
    void regeneratingAfterCollidingEntryIsRemovedRenamesKeyWhenOverwriteIsConfirmed() {
        useKeyPreferences(false);

        generateKeysForSelectedEntries();

        verify(dialogService).showConfirmationDialogWithOptOutAndWait(anyString(), anyString(), anyString(), anyString(), anyString(), any());
        assertEquals(Optional.of("Wang2020"), remainingWang.getCitationKey());
    }

    @Test
    void regeneratingAfterCollidingEntryIsRemovedKeepsKeyWhenOverwriteIsAvoided() {
        useKeyPreferences(true);

        generateKeysForSelectedEntries();

        verify(dialogService, never()).showConfirmationDialogWithOptOutAndWait(anyString(), anyString(), anyString(), anyString(), anyString(), any());
        assertEquals(Optional.of("Wang2020a"), remainingWang.getCitationKey());
    }

    @Test
    void executeGeneratesKeyAfterPreviousExecutionWasCanceled() {
        useKeyPreferences(false);
        when(dialogService.showConfirmationDialogWithOptOutAndWait(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(false);

        GenerateCitationKeyAction action = new GenerateCitationKeyAction(
                () -> null, dialogService, stateManager, new CurrentThreadTaskExecutor(), preferences);

        JavaFxExtension.invokeAndWait(action::execute);
        JavaFxExtension.awaitEvents();

        assertEquals(Optional.of("Wang2020a"), remainingWang.getCitationKey());

        BibEntry entryWithoutKey = new BibEntry()
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.YEAR, "2026");
        stateManager.getActiveDatabase().get().getDatabase().insertEntry(entryWithoutKey);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entryWithoutKey));

        JavaFxExtension.invokeAndWait(action::execute);
        JavaFxExtension.awaitEvents();

        assertEquals(Optional.of("Smith2026"), entryWithoutKey.getCitationKey());
    }

    @Test
    void delayedTaskGeneratesKeyWhenSubsequentExecutionIsCanceled() {
        useKeyPreferences(false);
        when(dialogService.showConfirmationDialogWithOptOutAndWait(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(false);

        List<Runnable> queuedTasks = new ArrayList<>();
        TaskExecutor delayingTaskExecutor = new CurrentThreadTaskExecutor() {
            @Override
            public <V> Future<V> execute(BackgroundTask<V> task) {
                queuedTasks.add(() -> super.execute(task));
                return CompletableFuture.completedFuture(null);
            }
        };

        GenerateCitationKeyAction action = new GenerateCitationKeyAction(
                () -> null, dialogService, stateManager, delayingTaskExecutor, preferences);

        BibEntry entryWithoutKey = new BibEntry()
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.YEAR, "2026");
        stateManager.getActiveDatabase().get().getDatabase().insertEntry(entryWithoutKey);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entryWithoutKey));

        JavaFxExtension.invokeAndWait(action::execute);
        JavaFxExtension.awaitEvents();

        assertEquals(Optional.empty(), entryWithoutKey.getCitationKey());

        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(remainingWang));

        JavaFxExtension.invokeAndWait(action::execute);
        JavaFxExtension.awaitEvents();

        for (Runnable task : queuedTasks) {
            JavaFxExtension.invokeAndWait(task);
        }
        JavaFxExtension.awaitEvents();

        assertEquals(Optional.of("Smith2026"), entryWithoutKey.getCitationKey());
        assertEquals(Optional.of("Wang2020a"), remainingWang.getCitationKey());
    }
}
