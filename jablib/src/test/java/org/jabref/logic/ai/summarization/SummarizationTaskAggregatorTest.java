package org.jabref.logic.ai.summarization;

import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.summarization.logic.BibEntrySummarizator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@NullMarked
class SummarizationTaskAggregatorTest {

    private SummarizationTaskAggregator aggregator;
    private InMemorySummaryCache cache;
    private FullBibEntry fullEntry;

    @BeforeEach
    void setUp() {
        cache = new InMemorySummaryCache(mock(SummariesRepository.class));
        aggregator = new SummarizationTaskAggregator(mock(TaskExecutor.class), cache);
        fullEntry = new FullBibEntry(new BibDatabaseContext(), new BibEntry());
    }

    private GenerateSummaryTaskRequest request(boolean regenerate) {
        return new GenerateSummaryTaskRequest(mock(FilePreferences.class), mock(ChatModel.class), mock(Summarizator.class), fullEntry, regenerate);
    }

    @Test
    void regenerateReplacesFailedTask() {
        GenerateSummaryTask failed = aggregator.start(request(true));
        // No linked files, so summarizing fails
        assertThrows(RuntimeException.class, failed::call);
        assertEquals(TrackedBackgroundTask.Status.ERROR, failed.getStatus());

        assertNotSame(failed, aggregator.start(request(true)));
    }

    @Test
    void finishedTaskDoesNotUnregisterItsReplacement() {
        GenerateSummaryTask failed = aggregator.start(request(true));
        assertThrows(RuntimeException.class, failed::call);
        GenerateSummaryTask replacement = aggregator.start(request(true));

        // The finish callback of the failed task runs only now
        failed.getOnException().accept(new RuntimeException());

        assertSame(replacement, aggregator.getTask(fullEntry.entry()).orElse(null));
    }

    @Test
    void startReusesRunningTask() {
        GenerateSummaryTask running = aggregator.start(request(false));

        assertSame(running, aggregator.start(request(true)));
    }

    @Test
    void delayedSuccessCallbackCannotRestoreSummaryAfterRegenerationStarts() throws Exception {
        AiSummary oldSummary = mock(AiSummary.class);
        AiSummary newSummary = mock(AiSummary.class);

        try (MockedConstruction<BibEntrySummarizator> ignored = mockConstruction(BibEntrySummarizator.class,
                (summarizator, context) -> when(summarizator.summarize(any(), any(), any()))
                        .thenReturn(context.getCount() == 1 ? oldSummary : newSummary))) {
            GenerateSummaryTask oldTask = aggregator.start(request(true));
            assertSame(oldSummary, oldTask.call());

            GenerateSummaryTask replacement = aggregator.start(request(true));
            cache.remove(fullEntry.entry());

            oldTask.getOnSuccess().accept(oldSummary);
            assertEquals(Optional.empty(), cache.get(fullEntry.entry()));
            assertSame(replacement, aggregator.getTask(fullEntry.entry()).orElse(null));

            assertSame(newSummary, replacement.call());
            replacement.getOnSuccess().accept(newSummary);
            assertEquals(Optional.of(newSummary), cache.get(fullEntry.entry()));
        }
    }
}
