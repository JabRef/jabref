package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AbbreviationsTest {

    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = JournalAbbreviationLoader.loadBuiltInRepository();
    }

    @Test
    void failedInitialLoadInstallsFallbackWithoutBuiltInEntries() {
        AbbreviationPreferences preferences = AbbreviationPreferences.getDefault();
        preferences.setUseBuiltInJournalList(false);
        try (var loader = mockStatic(JournalAbbreviationLoader.class, CALLS_REAL_METHODS)) {
            loader.when(() -> JournalAbbreviationLoader.loadRepository(any())).thenThrow(new IllegalArgumentException("Load failed"));
            JournalAbbreviationRepository retained = JournalAbbreviationLoader.loadRepositoryInBackground(preferences, new CurrentThreadTaskExecutor());
            assertEquals(Optional.empty(), retained.get("Demonstration"));
            assertEquals(Optional.empty(), retained.get("2D Materials"));
            retained.addCustomAbbreviation(new Abbreviation("Custom Journal", "Custom J."));
            assertEquals(Optional.of("Custom J."), retained.getDefaultAbbreviation("Custom Journal"));
        }
    }

    @Test
    void reloadPreservesAddedCustomAbbreviations() {
        Abbreviation custom = new Abbreviation("Custom Journal", "Custom J.");
        repository.addCustomAbbreviation(custom);
        repository.reload(AbbreviationPreferences.getDefault(), new CurrentThreadTaskExecutor());
        assertEquals(Optional.of("Custom J."), repository.getDefaultAbbreviation("Custom Journal"));
    }

    @Test
    void failedWriteDoesNotPreventOtherFilesOrReload(@TempDir Path directory) throws IOException {
        Path failed = directory.resolve("missing").resolve("failed.csv");
        Path valid = directory.resolve("valid.csv");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(failed.toString(), valid.toString()), true, false);
        preferences.setUseBuiltInJournalList(false);
        Abbreviation abbreviation = new Abbreviation("External Journal", "Ext. J.");
        repository.saveJournalLists(Map.of(failed, List.of(abbreviation), valid, List.of(abbreviation)),
                preferences, new CurrentThreadTaskExecutor(), () -> {
                });
        assertEquals(List.of(abbreviation), List.copyOf(JournalAbbreviationLoader.readAbbreviationsFromCsvFile(valid)));
        assertEquals(Optional.of(List.of(abbreviation)), repository.getPendingJournalList(failed));
        assertEquals(Optional.empty(), repository.getPendingJournalList(valid));
        assertEquals(Optional.empty(), repository.getDefaultAbbreviation("2D Materials"));
    }

    @Test
    void removedListIsNotWrittenByLaterSave(@TempDir Path directory) {
        Path removed = directory.resolve("removed.csv");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(removed.toString()), true, false);
        List<Runnable> queued = new ArrayList<>();
        TaskExecutor executor = mock(TaskExecutor.class);
        when(executor.execute(any())).thenAnswer(invocation -> {
            BackgroundTask<?> task = invocation.getArgument(0);
            queued.add(() -> new CurrentThreadTaskExecutor().execute(task));
            return CompletableFuture.completedFuture(null);
        });
        repository.saveJournalLists(Map.of(removed, List.of(new Abbreviation("Journal", "J."))), preferences, executor, () -> {
        });
        preferences.setExternalJournalLists(List.of());
        repository.saveJournalLists(Map.of(), preferences, executor, () -> {
        });
        queued.get(1).run();
        queued.getFirst().run();
        assertFalse(Files.exists(removed));
        assertEquals(Optional.empty(), repository.getPendingJournalList(removed));
    }

    @Test
    void getNextAbbreviationAbbreviatesJournalTitle() {
        assertEquals("2D Mater.", repository.getNextAbbreviation("2D Materials").get());
    }

    @Test
    void getNextAbbreviationConvertsAbbreviationToDotlessAbbreviation() {
        assertEquals("2D Mater", repository.getNextAbbreviation("2D Mater.").get());
    }

    @Test
    void disablingBuiltInListRetainsExternalAbbreviations(@TempDir Path tempDir) throws IOException {
        // [utest->req~journals.disable-built-in~1]
        Path externalList = tempDir.resolve("journals.csv");
        Files.writeString(externalList, "External Journal;Ext. J.;Ext. J.");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(externalList.toString()), true, false);

        JournalAbbreviationRepository enabled = JournalAbbreviationLoader.loadRepository(preferences);
        assertEquals(Optional.of("2D Mater."), enabled.getDefaultAbbreviation("2D Materials"));
        assertEquals(Optional.of("Ext. J."), enabled.getDefaultAbbreviation("External Journal"));

        preferences.setUseBuiltInJournalList(false);
        JournalAbbreviationRepository disabled = JournalAbbreviationLoader.loadRepository(preferences);
        assertEquals(Optional.empty(), disabled.getDefaultAbbreviation("2D Materials"));
        assertEquals(Optional.empty(), disabled.get("2D Mater."));
        assertEquals(Optional.of("Ext. J."), disabled.getDefaultAbbreviation("External Journal"));
        assertEquals(Optional.of("External Journal"), disabled.get("Ext. J.").map(Abbreviation::getName));

        preferences.setUseBuiltInJournalList(true);
        JournalAbbreviationRepository reenabled = JournalAbbreviationLoader.loadRepository(preferences);
        assertEquals(Optional.of("2D Mater."), reenabled.getDefaultAbbreviation("2D Materials"));
        assertEquals(Optional.of("Ext. J."), reenabled.getDefaultAbbreviation("External Journal"));
    }

    @Test
    void disabledExternalListIsExcludedWithoutChangingPrecedence(@TempDir Path directory) throws IOException {
        // [utest->req~journals.disable-external~1]
        Path first = directory.resolve("first.csv");
        Path second = directory.resolve("second.csv");
        Files.writeString(first, "External Journal;First abbreviation;First abbreviation");
        Files.writeString(second, "External Journal;Second abbreviation;Second abbreviation");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(first.toString(), second.toString()), true, false);
        TaskExecutor executor = new CurrentThreadTaskExecutor();

        repository.reload(preferences, executor);
        assertEquals(Optional.of("First abbreviation"), repository.getDefaultAbbreviation("External Journal"));

        preferences.setDisabledExternalJournalLists(List.of(first.toString()));
        repository.reload(preferences, executor);
        assertEquals(Optional.of("Second abbreviation"), repository.getDefaultAbbreviation("External Journal"));
        assertEquals(Optional.empty(), repository.get("First abbreviation"));
        assertEquals(Optional.of("External Journal"), repository.get("Second abbreviation").map(Abbreviation::getName));
        assertEquals(Optional.of("2D Mater."), repository.getDefaultAbbreviation("2D Materials"));

        preferences.setDisabledExternalJournalLists(List.of(first.toString(), second.toString()));
        preferences.setUseBuiltInJournalList(false);
        repository.reload(preferences, executor);
        assertEquals(Optional.empty(), repository.getDefaultAbbreviation("External Journal"));
        assertEquals(Optional.empty(), repository.getDefaultAbbreviation("2D Materials"));

        preferences.setDisabledExternalJournalLists(List.of());
        repository.reload(preferences, executor);
        assertEquals(Optional.of("First abbreviation"), repository.getDefaultAbbreviation("External Journal"));
        assertEquals(List.of(first.toString(), second.toString()), preferences.getExternalJournalLists());
    }

    @Test
    void externalListReloadCapturesDisabledStateBeforeBackgroundExecution(@TempDir Path directory) throws IOException {
        Path external = directory.resolve("external.csv");
        Files.writeString(external, "External Journal;Ext. J.;Ext. J.");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(external.toString()), true, false);
        preferences.setDisabledExternalJournalLists(List.of(external.toString()));
        List<Runnable> queued = new ArrayList<>();
        TaskExecutor executor = mock(TaskExecutor.class);
        when(executor.execute(any())).thenAnswer(invocation -> {
            BackgroundTask<?> task = invocation.getArgument(0);
            queued.add(() -> new CurrentThreadTaskExecutor().execute(task));
            return CompletableFuture.completedFuture(null);
        });

        repository.reload(preferences, executor);
        preferences.setDisabledExternalJournalLists(List.of());
        queued.removeFirst().run();
        assertEquals(List.of(), repository.getFullNames().stream().filter("External Journal"::equals).toList());

        repository.reload(preferences, executor);
        queued.removeFirst().run();
        assertEquals(Optional.of("Ext. J."), repository.getDefaultAbbreviation("External Journal"));
    }

    @Test
    void backgroundLoadedRepositoryReturnsBuiltInAbbreviations() {
        JournalAbbreviationRepository backgroundLoadedRepository = JournalAbbreviationLoader.loadRepositoryInBackground(
                new AbbreviationPreferences(java.util.List.of(), true, false),
                new CurrentThreadTaskExecutor()
        );

        assertNotNull(backgroundLoadedRepository);
        assertEquals("2D Mater.", backgroundLoadedRepository.getNextAbbreviation("2D Materials").get());
    }

    @Test
    void backgroundLoadedRepositoryDoesNotStartLoadingBeforeFirstUse() {
        RecordingTaskExecutor taskExecutor = new RecordingTaskExecutor();

        JournalAbbreviationRepository backgroundLoadedRepository = JournalAbbreviationLoader.loadRepositoryInBackground(
                new AbbreviationPreferences(java.util.List.of(), true, false),
                taskExecutor
        );

        assertNotNull(backgroundLoadedRepository);
        assertEquals(0, taskExecutor.executionCount);

        assertThrows(UnsupportedOperationException.class, () -> backgroundLoadedRepository.getNextAbbreviation("2D Materials"));

        assertEquals(1, taskExecutor.executionCount);
    }

    @Test
    void savedToggleRefreshesExistingRepositoryReference() {
        AbbreviationPreferences preferences = AbbreviationPreferences.getDefault();
        TaskExecutor executor = new CurrentThreadTaskExecutor();
        JournalAbbreviationRepository retained = JournalAbbreviationLoader.loadRepositoryInBackground(preferences, executor);
        assertEquals(Optional.of("2D Mater."), retained.getDefaultAbbreviation("2D Materials"));

        preferences.setUseBuiltInJournalList(false);
        retained.reload(preferences, executor);
        assertEquals(Optional.empty(), retained.getDefaultAbbreviation("2D Materials"));
        assertEquals(Optional.empty(), retained.get("2D Mater."));

        preferences.setUseBuiltInJournalList(true);
        retained.reload(preferences, executor);
        assertEquals(Optional.of("2D Mater."), retained.getDefaultAbbreviation("2D Materials"));
    }

    @Test
    void olderInitialLoadCannotOverwriteSavedToggle() {
        AbbreviationPreferences preferences = AbbreviationPreferences.getDefault();
        List<Runnable> queued = new ArrayList<>();
        TaskExecutor executor = mock(TaskExecutor.class);
        when(executor.execute(any())).thenAnswer(invocation -> {
            BackgroundTask<?> task = invocation.getArgument(0);
            queued.add(() -> new CurrentThreadTaskExecutor().execute(task));
            return CompletableFuture.completedFuture(null);
        });
        JournalAbbreviationRepository retained = JournalAbbreviationLoader.loadRepositoryInBackground(preferences, executor);
        assertEquals(Optional.empty(), retained.getDefaultAbbreviation("2D Materials"));
        preferences.setUseBuiltInJournalList(false);
        retained.reload(preferences, executor);
        assertEquals(2, queued.size());

        queued.getLast().run();
        queued.getFirst().run();
        assertEquals(Optional.empty(), retained.getDefaultAbbreviation("2D Materials"));
        assertEquals(Optional.empty(), retained.get("Demonstration"));
    }

    @Test
    void disablingBuiltInListRetainsLtwa() {
        AbbreviationPreferences preferences = AbbreviationPreferences.getDefault();
        preferences.setUseBuiltInJournalList(false);
        JournalAbbreviationRepository disabled = JournalAbbreviationLoader.loadRepository(preferences);
        assertEquals(repository.getLtwaAbbreviation("Physical Review Letters"), disabled.getLtwaAbbreviation("Physical Review Letters"));
        assertFalse(disabled.getLtwaAbbreviation("Physical Review Letters").isEmpty());
    }

    @Test
    void disabledFallbackDoesNotRestoreDemonstrationOrBuiltInEntries(@TempDir Path tempDir) throws IOException {
        Path external = tempDir.resolve("external.csv");
        Files.writeString(external, "External Journal;Ext. J.;Ext. J.");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(external.toString()), true, false);
        preferences.setUseBuiltInJournalList(false);
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.createTempDirectory("jabref-ltwa")).thenThrow(new IOException("Test LTWA loading failure"));
            JournalAbbreviationRepository disabled = JournalAbbreviationLoader.loadRepositoryInBackground(preferences, new CurrentThreadTaskExecutor());
            assertEquals(Optional.empty(), disabled.get("Demonstration"));
            assertEquals(Optional.empty(), disabled.get("2D Materials"));
            assertEquals(Optional.of("Ext. J."), disabled.getDefaultAbbreviation("External Journal"));
        }
    }

    @Test
    void externalEntryOverridesBuiltInEntryRegardlessOfToggle(@TempDir Path tempDir) throws IOException {
        Path external = tempDir.resolve("external.csv");
        Files.writeString(external, "2D Materials;Custom abbreviation;Custom abbreviation");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(external.toString()), true, false);
        for (boolean enabled : List.of(true, false, true)) {
            preferences.setUseBuiltInJournalList(enabled);
            JournalAbbreviationRepository loaded = JournalAbbreviationLoader.loadRepository(preferences);
            assertEquals(Optional.of("Custom abbreviation"), loaded.getDefaultAbbreviation("2D Materials"));
            assertEquals(Optional.of("2D Materials"), loaded.get("Custom abbreviation").map(Abbreviation::getName));
            assertEquals(List.of(external.toString()), preferences.getExternalJournalLists());
        }
    }

    private static class RecordingTaskExecutor implements TaskExecutor {
        private int executionCount = 0;

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            executionCount++;
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public DelayTaskThrottler createThrottler(int delay) {
            throw new UnsupportedOperationException();
        }
    }
}
