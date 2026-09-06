package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// [utest->req~entry-editor.main-tab.autolink-suggestions~1]
@ExtendWith(ApplicationExtension.class)
class AllFieldsTabTest {

    /// Runs probes synchronously, but can hold them back so a test can change entry state
    /// between probe start and probe completion (the stale-result scenario).
    private static class DeferringTaskExecutor extends CurrentThreadTaskExecutor {
        private final List<BackgroundTask<?>> deferred = new ArrayList<>();
        private boolean deferring;

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            if (deferring) {
                deferred.add(task);
                return CompletableFuture.completedFuture(null);
            }
            return super.execute(task);
        }

        void deferUpcomingTasks() {
            deferring = true;
        }

        /// Later tasks (e.g. the probe restarted by the key change itself) stay deferred.
        void runNextDeferredTask() {
            super.execute(deferred.removeFirst());
        }
    }

    private Path fileDirectory;
    private GuiPreferences preferences;
    private DeferringTaskExecutor taskExecutor;
    private AllFieldsTab tab;

    @BeforeEach
    void setUp(@TempDir Path fileDirectory) {
        this.fileDirectory = fileDirectory;

        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getOwnerPreferences().getDefaultOwner()).thenReturn("owner");
        when(preferences.getEntryEditorPreferences().autoLinkFilesEnabled()).thenReturn(true);
        when(preferences.getCitationKeyPatternPreferences().getUnwantedCharacters()).thenReturn("");
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
        when(preferences.getAutoLinkPreferences()).thenReturn(
                new AutoLinkPreferences(AutoLinkPreferences.CitationKeyDependency.START, "", false, ';'));

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getFileDirectories(any())).thenReturn(List.of(fileDirectory));
        when(databaseContext.getMode()).thenReturn(BibDatabaseMode.BIBTEX);
        when(databaseContext.getMetaData()).thenReturn(new MetaData());

        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        when(stateManager.getUndoManager(any())).thenReturn(new HeadlessGuiUndoManager());

        taskExecutor = new DeferringTaskExecutor();
        Injector.setModelOrService(TaskExecutor.class, taskExecutor);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(UndoManager.class, new JabRefUndoManager());
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        Injector.setModelOrService(BibEntryTypesManager.class, new BibEntryTypesManager());
        Injector.setModelOrService(JournalAbbreviationRepository.class, mock(JournalAbbreviationRepository.class));
        Injector.setModelOrService(FileUpdateMonitor.class, new DummyFileUpdateMonitor());

        tab = new AllFieldsTab(
                mock(UndoAction.class),
                mock(RedoAction.class),
                preferences,
                new BibEntryTypesManager(),
                mock(JournalAbbreviationRepository.class),
                stateManager,
                mock(PreviewPanel.class));
    }

    private void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            action.run();
            done.countDown();
        });
        assertTrue(done.await(30, TimeUnit.SECONDS));
    }

    @Test
    void fileEditorAppearsWhenAutolinkFindsUnlinkedFile() throws IOException, InterruptedException {
        Files.createFile(fileDirectory.resolve("CiteKey2021.pdf"));
        BibEntry entry = new BibEntry(StandardEntryType.Misc).withCitationKey("CiteKey2021");

        runOnFxThreadAndWait(() -> tab.bindToEntry(entry));

        assertTrue(tab.editors.containsKey(StandardField.FILE));
    }

    @Test
    void fileEditorStaysHiddenWithoutMatchingFile() throws InterruptedException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc).withCitationKey("CiteKey2021");

        runOnFxThreadAndWait(() -> tab.bindToEntry(entry));

        assertFalse(tab.editors.containsKey(StandardField.FILE));
    }

    @Test
    void fileEditorStaysHiddenWhenAutolinkIsDisabled() throws IOException, InterruptedException {
        when(preferences.getEntryEditorPreferences().autoLinkFilesEnabled()).thenReturn(false);
        Files.createFile(fileDirectory.resolve("CiteKey2021.pdf"));
        BibEntry entry = new BibEntry(StandardEntryType.Misc).withCitationKey("CiteKey2021");

        runOnFxThreadAndWait(() -> tab.bindToEntry(entry));

        assertFalse(tab.editors.containsKey(StandardField.FILE));
    }

    @Test
    void staleProbeResultDoesNotAddFileEditor() throws IOException, InterruptedException {
        Files.createFile(fileDirectory.resolve("CiteKey2021.pdf"));
        BibEntry entry = new BibEntry(StandardEntryType.Misc).withCitationKey("OtherKey");

        taskExecutor.deferUpcomingTasks();
        runOnFxThreadAndWait(() -> tab.bindToEntry(entry));
        // The probe for "OtherKey" is still pending; by the time it runs, the key has changed
        // and its (now matching) result must be discarded.
        runOnFxThreadAndWait(() -> {
            entry.setCitationKey("CiteKey2021");
            taskExecutor.runNextDeferredTask();
        });

        assertFalse(tab.editors.containsKey(StandardField.FILE));
    }
}
