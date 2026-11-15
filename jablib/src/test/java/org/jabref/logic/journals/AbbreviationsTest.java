package org.jabref.logic.journals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.logic.journals.ltwa.LtwaRepository;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;

import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

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
        QueuedTaskExecutor executor = new QueuedTaskExecutor();
        repository.saveJournalLists(Map.of(removed, List.of(new Abbreviation("Journal", "J."))), preferences, executor, () -> {
        });
        preferences.setExternalJournalLists(List.of());
        repository.saveJournalLists(Map.of(), preferences, executor, () -> {
        });
        executor.runLast();
        executor.runNext();
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
        QueuedTaskExecutor executor = new QueuedTaskExecutor();

        repository.reload(preferences, executor);
        preferences.setDisabledExternalJournalLists(List.of());
        executor.runNext();
        assertEquals(List.of(), repository.getFullNames().stream().filter("External Journal"::equals).toList());

        repository.reload(preferences, executor);
        executor.runNext();
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
        QueuedTaskExecutor executor = new QueuedTaskExecutor();
        JournalAbbreviationRepository retained = JournalAbbreviationLoader.loadRepositoryInBackground(preferences, executor);
        assertEquals(Optional.empty(), retained.getDefaultAbbreviation("2D Materials"));
        preferences.setUseBuiltInJournalList(false);
        retained.reload(preferences, executor);
        assertEquals(2, executor.queued.size());

        executor.runLast();
        executor.runNext();
        assertEquals(Optional.empty(), retained.getDefaultAbbreviation("2D Materials"));
        assertEquals(Optional.empty(), retained.get("Demonstration"));
    }

    @Test
    void newerReloadCancelsQueuedTaskBeforeLoading() {
        // [utest->req~journals.cancel-obsolete-reloads~1]
        AbbreviationPreferences preferences = AbbreviationPreferences.getDefault();
        QueuedTaskExecutor executor = new QueuedTaskExecutor();
        JournalAbbreviationRepository latest = new JournalAbbreviationRepository(new LtwaRepository());
        latest.addLoadedAbbreviations(List.of(new Abbreviation("Latest", "Latest abbreviation")));
        try (var loader = mockStatic(JournalAbbreviationLoader.class)) {
            loader.when(() -> JournalAbbreviationLoader.loadRepository(any())).thenReturn(latest);
            repository.reload(preferences, executor);
            BackgroundTask<?> older = executor.queued.getFirst();
            repository.reload(preferences, executor);
            BackgroundTask<?> intermediate = executor.queued.getLast();
            repository.reload(preferences, executor);
            assertTrue(older.isCancelled());
            assertTrue(intermediate.isCancelled());

            executor.runNext();
            executor.runNext();
            loader.verifyNoInteractions();
            executor.runNext();
            loader.verify(() -> JournalAbbreviationLoader.loadRepository(any()), times(1));
            assertEquals(Optional.of("Latest abbreviation"), repository.getDefaultAbbreviation("Latest"));
        }
    }

    @Test
    void completedReloadIsReleasedBeforeNextRequest() {
        QueuedTaskExecutor executor = new QueuedTaskExecutor();
        try (var loader = mockStatic(JournalAbbreviationLoader.class)) {
            loader.when(() -> JournalAbbreviationLoader.loadRepository(any()))
                  .thenReturn(new JournalAbbreviationRepository(new LtwaRepository()));
            repository.reload(AbbreviationPreferences.getDefault(), executor);
            BackgroundTask<?> completed = executor.queued.getFirst();
            executor.runNext();

            repository.reload(AbbreviationPreferences.getDefault(), executor);
            assertFalse(completed.isCancelled());
            executor.runNext();
        }
    }

    @Test
    void failedReloadIsReleasedBeforeNextRequest() {
        QueuedTaskExecutor executor = new QueuedTaskExecutor();
        try (var loader = mockStatic(JournalAbbreviationLoader.class)) {
            loader.when(() -> JournalAbbreviationLoader.loadRepository(any()))
                  .thenThrow(new IllegalArgumentException("Load failed"))
                  .thenReturn(new JournalAbbreviationRepository(new LtwaRepository()));
            repository.reload(AbbreviationPreferences.getDefault(), executor);
            BackgroundTask<?> failed = executor.queued.getFirst();
            executor.runNext();
            assertEquals(Optional.of("2D Mater."), repository.getDefaultAbbreviation("2D Materials"));

            repository.reload(AbbreviationPreferences.getDefault(), executor);
            assertFalse(failed.isCancelled());
            executor.runNext();
            assertEquals(Optional.empty(), repository.getDefaultAbbreviation("2D Materials"));
        }
    }

    @Test
    void canceledRunningReloadMustFinishBeforeAnotherLoadStarts() throws InterruptedException, ExecutionException, TimeoutException {
        // [utest->req~journals.cancel-obsolete-reloads~1]
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger activeLoads = new AtomicInteger();
        AtomicInteger maximumActiveLoads = new AtomicInteger();
        JournalAbbreviationRepository latest = new JournalAbbreviationRepository(new LtwaRepository());
        latest.addLoadedAbbreviations(List.of(new Abbreviation("Latest", "Latest abbreviation")));

        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            ParallelReloadExecutor executor = new ParallelReloadExecutor(workers, () -> {
                int load = loads.incrementAndGet();
                maximumActiveLoads.accumulateAndGet(activeLoads.incrementAndGet(), Math::max);
                try {
                    if (load == 1) {
                        firstStarted.countDown();
                        assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
                    }
                    return latest;
                } finally {
                    activeLoads.decrementAndGet();
                }
            });
            try {
                repository.reload(AbbreviationPreferences.getDefault(), executor);
                assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
                repository.reload(AbbreviationPreferences.getDefault(), executor);
                assertTrue(executor.secondWorkerStarted.await(5, TimeUnit.SECONDS));

                assertThrows(TimeoutException.class, () -> executor.submitted.getLast().get(100, TimeUnit.MILLISECONDS));
            } finally {
                releaseFirst.countDown();
            }
            for (Future<?> submitted : executor.submitted) {
                submitted.get(5, TimeUnit.SECONDS);
            }
            assertEquals(1, maximumActiveLoads.get());
            assertEquals(2, loads.get());
            assertEquals(Optional.of("Latest abbreviation"), repository.getDefaultAbbreviation("Latest"));
        }
    }

    @Test
    void bundledLtwaIsLoadedLazilyAndReused() {
        // [utest->req~journals.reuse-ltwa~1]
        // [utest->adr~share-bundled-ltwa~1]
        AtomicInteger loads = new AtomicInteger();
        LtwaRepository expected = new LtwaRepository();
        var cache = new JournalAbbreviationLoader.LtwaRepositoryCache(() -> {
            loads.incrementAndGet();
            return expected;
        });
        assertEquals(0, loads.get());

        assertSame(expected, cache.get());
        assertSame(expected, cache.get());
        assertEquals(1, loads.get());
    }

    @Test
    void failedLtwaInitializationCanBeRetried() {
        AtomicInteger loads = new AtomicInteger();
        LtwaRepository expected = new LtwaRepository();
        var cache = new JournalAbbreviationLoader.LtwaRepositoryCache(Unchecked.supplier(() -> {
            if (loads.incrementAndGet() == 1) {
                throw new IOException("LTWA loading failed");
            }
            return expected;
        }));

        assertThrows(UncheckedIOException.class, cache::get);
        assertSame(expected, cache.get());
        assertSame(expected, cache.get());
        assertEquals(2, loads.get());
    }

    @Test
    void concurrentLtwaLoadsShareOneInitialization() throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CompletableFuture<Boolean> releaseFirst = new CompletableFuture<>();
        AtomicInteger loads = new AtomicInteger();
        LtwaRepository expected = new LtwaRepository();
        var cache = new JournalAbbreviationLoader.LtwaRepositoryCache(() -> {
            loads.incrementAndGet();
            firstStarted.countDown();
            releaseFirst.join();
            return expected;
        });
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<LtwaRepository> first = workers.submit(cache::get);
            Future<LtwaRepository> second;
            try {
                assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
                second = workers.submit(() -> {
                    secondStarted.countDown();
                    return cache.get();
                });
                assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            } finally {
                releaseFirst.complete(true);
            }
            assertSame(expected, first.get(5, TimeUnit.SECONDS));
            assertSame(expected, second.get(5, TimeUnit.SECONDS));
            assertEquals(1, loads.get());
        }
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
        try (var loader = mockStatic(JournalAbbreviationLoader.class, CALLS_REAL_METHODS)) {
            loader.when(JournalAbbreviationLoader::loadLtwaRepository).thenThrow(new UncheckedIOException(new IOException("Test LTWA loading failure")));
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

    /// Keeps repository tasks outside Mockito's invocation history so completed fixtures can be collected.
    @NullMarked
    private static class QueuedTaskExecutor extends CurrentThreadTaskExecutor {
        private final List<BackgroundTask<?>> queued = new ArrayList<>();

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            queued.add(task);
            return CompletableFuture.completedFuture(null);
        }

        private void runNext() {
            super.execute(queued.removeFirst());
        }

        private void runLast() {
            super.execute(queued.removeLast());
        }
    }

    @NullMarked
    private static class ParallelReloadExecutor extends CurrentThreadTaskExecutor {
        private final ExecutorService workers;
        private final Callable<JournalAbbreviationRepository> load;
        private final List<Future<?>> submitted = new ArrayList<>();
        private final CountDownLatch secondWorkerStarted = new CountDownLatch(1);

        private ParallelReloadExecutor(ExecutorService workers, Callable<JournalAbbreviationRepository> load) {
            this.workers = workers;
            this.load = load;
        }

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            int index = submitted.size();
            Future<V> future = workers.submit(() -> {
                try (var loader = mockStatic(JournalAbbreviationLoader.class)) {
                    loader.when(() -> JournalAbbreviationLoader.loadRepository(any())).thenAnswer(_ -> load.call());
                    if (index == 1) {
                        secondWorkerStarted.countDown();
                    }
                    return super.execute(task).get();
                }
            });
            submitted.add(future);
            return future;
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
