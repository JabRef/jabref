package org.jabref.logic.ai.ingestion;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javafx.util.Pair;

import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.ingestion.tasks.generateembeddingsforseveral.GenerateEmbeddingsForSeveralTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddingsforseveral.GenerateEmbeddingsForSeveralTaskRequest;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.LinkedFile;

public class IngestionTaskAggregator {
    private final TaskExecutor taskExecutor;

    private final Map<LinkedFile, Pair<Future<Void>, GenerateEmbeddingsTask>> generateEmbeddingsTasks =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private final Map<GenerateEmbeddingsForSeveralTaskRequest, Pair<Future<Void>, GenerateEmbeddingsForSeveralTask>> generateEmbeddingsForSeveralTasks =
            Collections.synchronizedMap(new HashMap<>());

    public IngestionTaskAggregator(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public synchronized Pair<Future<Void>, GenerateEmbeddingsTask> startWithFuture(GenerateEmbeddingsTaskRequest request) {
        return generateEmbeddingsTasks.computeIfAbsent(request.linkedFile(), _ -> {
            GenerateEmbeddingsTask task = new GenerateEmbeddingsTask(request);
            task.onFinished(() -> generateEmbeddingsTasks.remove(request.linkedFile()));
            Future<Void> future = taskExecutor.execute(task);
            return new Pair<>(future, task);
        });
    }

    public synchronized GenerateEmbeddingsTask start(GenerateEmbeddingsTaskRequest request) {
        return startWithFuture(request).getValue();
    }

    public synchronized Pair<Future<Void>, GenerateEmbeddingsForSeveralTask> start(GenerateEmbeddingsForSeveralTaskRequest request) {
        return generateEmbeddingsForSeveralTasks.computeIfAbsent(request, _ -> {
            GenerateEmbeddingsForSeveralTask task = new GenerateEmbeddingsForSeveralTask(this, request);
            task.onFinished(() -> generateEmbeddingsForSeveralTasks.remove(request));
            Future<Void> future = taskExecutor.execute(task);
            return new Pair<>(future, task);
        });
    }
}
