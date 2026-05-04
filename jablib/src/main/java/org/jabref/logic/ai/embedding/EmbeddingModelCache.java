package org.jabref.logic.ai.embedding;

import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;

/// Session-scoped cache for {@link AsyncEmbeddingModel} instances, keyed by {@link PredefinedEmbeddingModel}.
///
/// When multiple components request an embedding model for the same {@link PredefinedEmbeddingModel},
/// this cache ensures only *one* {@link AsyncEmbeddingModel} instance—and therefore only one
/// background download/load task—is ever created for that model kind.
///
/// Without this cache, each caller that independently reacts to preference changes would
/// instantiate its own {@link AsyncEmbeddingModel}, spawning a duplicate
/// {@link org.jabref.logic.ai.ingestion.tasks.UpdateEmbeddingModelTask} for every listener.
///
/// Implements {@link AutoCloseable}; call {@link #close()} (e.g. in `AiService.close()`)
/// to release all cached model resources.
public class EmbeddingModelCache implements AutoCloseable {

    private final Map<PredefinedEmbeddingModel, AsyncEmbeddingModel> cache = new HashMap<>();

    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;

    public EmbeddingModelCache(NotificationService notificationService, TaskExecutor taskExecutor) {
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;
    }

    /// Returns the cached {@link AsyncEmbeddingModel} for `kind`, creating it on first access.
    ///
    /// Calling this method multiple times with the same `kind` always returns the
    /// *same* instance; no additional background tasks are launched.
    ///
    /// @param kind the requested embedding model kind
    /// @return a (possibly still-loading) {@link AsyncEmbeddingModel} for `kind`
    public AsyncEmbeddingModel getOrCreate(PredefinedEmbeddingModel kind) {
        return cache.computeIfAbsent(kind,
                k -> new AsyncEmbeddingModel(k, notificationService, taskExecutor));
    }

    /// Closes all cached {@link AsyncEmbeddingModel} instances and clears the cache.
    ///
    /// Should be called once the AI subsystem is shut down (i.e. from `AiService.close()`).
    @Override
    public void close() {
        cache.values().forEach(AsyncEmbeddingModel::close);
        cache.clear();
    }
}
