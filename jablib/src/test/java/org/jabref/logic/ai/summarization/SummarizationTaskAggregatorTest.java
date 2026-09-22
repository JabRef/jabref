package org.jabref.logic.ai.summarization;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SummarizationTaskAggregatorTest {

    private SummarizationTaskAggregator aggregator;
    private FullBibEntry fullEntry;

    @BeforeEach
    void setUp() {
        aggregator = new SummarizationTaskAggregator(mock(TaskExecutor.class), mock(InMemorySummaryCache.class));
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
    void startReusesRunningTask() {
        GenerateSummaryTask running = aggregator.start(request(false));

        assertSame(running, aggregator.start(request(true)));
    }
}
