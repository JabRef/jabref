package org.jabref.logic.journals;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbbreviationsTest {

    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = JournalAbbreviationLoader.loadBuiltInRepository();
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
