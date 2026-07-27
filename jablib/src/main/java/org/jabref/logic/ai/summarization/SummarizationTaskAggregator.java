package org.jabref.logic.ai.summarization;

import java.util.Comparator;
import java.util.Optional;
import java.util.TreeMap;

import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

/// Deduplicates summarization tasks across the application lifetime.
///
/// Exactly one {@link GenerateSummaryTask} is created per {@link BibEntry} object reference
/// ({@link BibEntry#getId()} is the tree key). A second call for the same entry while a task is
/// still running returns the existing task — no duplicate generation.
///
/// When a task completes successfully, the result is written to the {@link InMemorySummaryCache}
/// so that callers who were not listening at that moment can still retrieve it. The task is then
/// removed from the internal map.
///
/// All public methods are `synchronized` to make them safe for concurrent access from
/// both the JavaFX thread and background task threads.
public class SummarizationTaskAggregator {

    private final TaskExecutor taskExecutor;
    private final InMemorySummaryCache inMemoryCache;

    private final TreeMap<BibEntry, GenerateSummaryTask> tasks =
            new TreeMap<>(Comparator.comparing(BibEntry::getId));

    public SummarizationTaskAggregator(TaskExecutor taskExecutor, InMemorySummaryCache inMemoryCache) {
        this.taskExecutor = taskExecutor;
        this.inMemoryCache = inMemoryCache;
    }

    /// Starts a {@link GenerateSummaryTask} for the entry in `request`, or returns the
    /// already-running task if one exists for that entry.
    ///
    /// `computeIfAbsent` is the deduplication mechanism — only one task per entry at a time.
    ///
    /// **Important:** if you attach a status listener to the returned task, also check
    /// {@link org.jabref.logic.ai.util.TrackedBackgroundTask#getStatus()} immediately after
    /// attaching, in case the task already finished before the listener was registered.
    public synchronized GenerateSummaryTask start(GenerateSummaryTaskRequest request) {
        Optional<GenerateSummaryTask> task = getTask(request.fullEntry().entry());

        if (task.isEmpty()) {
            return startNewTask(request);
        }

        if (task.get().getStatus() == TrackedBackgroundTask.Status.CANCELLED && request.regenerate()) {
            tasks.remove(request.fullEntry().entry());
            return startNewTask(request);
        }

        return task.get();
    }

    private synchronized GenerateSummaryTask startNewTask(GenerateSummaryTaskRequest request) {
        GenerateSummaryTask task = new GenerateSummaryTask(request);

        task.onFinished(() -> tasks.remove(request.fullEntry().entry()));

        task.onSuccess(result -> inMemoryCache.put(request.fullEntry(), result));

        tasks.put(request.fullEntry().entry(), task);

        taskExecutor.execute(task);
        return task;
    }

    public synchronized Optional<GenerateSummaryTask> getTask(BibEntry entry) {
        return Optional.ofNullable(tasks.get(entry));
    }
}
