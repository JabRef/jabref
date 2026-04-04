package org.jabref.logic.cleanup;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface CleanupJob {
    /// Cleans up the given entry and returns the list of changes made.
    List<FieldChange> cleanup(BibEntry entry);

    /// Cleans up the given entry, routing all {@link BibEntry} field mutations through the provided scheduler.
    ///
    /// The scheduler must execute mutations synchronously (blocking the caller until complete), e.g.
    /// {@code UiTaskExecutor::runAndWaitInJavaFXThread} when called from a background thread,
    /// or {@code Runnable::run} when already on the correct thread.
    ///
    /// The default implementation wraps the entire {@link #cleanup(BibEntry)} call in the scheduler, which
    /// is safe but puts all computation on the scheduler's thread. Override this method to keep expensive
    /// computation on the calling (background) thread and only schedule the actual field mutations.
    default List<FieldChange> cleanup(BibEntry entry, Consumer<Runnable> mutationScheduler) {
        AtomicReference<List<FieldChange>> result = new AtomicReference<>(List.of());
        mutationScheduler.accept(() -> result.set(cleanup(entry)));
        return result.get();
    }
}
