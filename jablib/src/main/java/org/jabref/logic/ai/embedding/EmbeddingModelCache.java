package org.jabref.logic.ai.embedding;

import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

/// Session-scoped cache for [AsyncEmbeddingModel] instances, keyed by embedding model name.
///
/// When multiple components request an embedding model for the same model name,
/// this cache ensures only *one* [AsyncEmbeddingModel] instance—and therefore only one
/// background download/load task—is ever created for that model name.
///
/// Without this cache, each caller that independently reacts to preference changes would
/// instantiate its own [AsyncEmbeddingModel], spawning a duplicate
/// [org.jabref.logic.ai.ingestion.tasks.UpdateEmbeddingModelTask] for every listener.
///
/// Implements [AutoCloseable]; call [#close()] (e.g. in `AiService.close()`)
/// to release all cached model resources.
public class EmbeddingModelCache implements AutoCloseable {

    private final Map<String, AsyncEmbeddingModel> cache = new HashMap<>();

    private final AiPreferences aiPreferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;
    private final EmbeddingModelMetadataService metadataService;

    public EmbeddingModelCache(
            AiPreferences aiPreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor,
            EmbeddingModelMetadataService metadataService) {
        this.aiPreferences = aiPreferences;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;
        this.metadataService = metadataService;
    }

    /// Returns the cached [AsyncEmbeddingModel] for `modelName`, creating it on first access.
    ///
    /// Calling this method multiple times with the same `modelName` always returns the
    /// *same* instance; no additional background tasks are launched.
    ///
    /// @param modelName the requested embedding model name
    /// @return a (possibly still-loading) [AsyncEmbeddingModel] for `modelName`
    public AsyncEmbeddingModel getOrCreate(String modelName) {
        return cache.computeIfAbsent(modelName,
                name -> new AsyncEmbeddingModel(name, aiPreferences, notificationService, taskExecutor, metadataService));
    }

    /// Closes all cached [AsyncEmbeddingModel] instances and clears the cache.
    ///
    /// Should be called once the AI subsystem is shut down (i.e. from `AiService.close()`).
    @Override
    public void close() {
        cache.values().forEach(AsyncEmbeddingModel::close);
        cache.clear();
    }
}
