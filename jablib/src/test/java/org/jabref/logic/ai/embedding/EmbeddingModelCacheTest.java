package org.jabref.logic.ai.embedding;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddingModelCacheTest {

    private static final String MODEL_A = "BAAI/bge-small-en-v1.5";
    private static final String MODEL_B = "intfloat/e5-small-v2";

    private EmbeddingModelCache cache;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        AiPreferences aiPreferences = mock(AiPreferences.class);
        when(aiPreferences.getAiFeaturesEnabled()).thenReturn(true);
        NotificationService notificationService = mock(NotificationService.class);
        taskExecutor = mock(TaskExecutor.class);
        when(taskExecutor.execute(any())).thenReturn(mock(java.util.concurrent.Future.class));

        cache = new EmbeddingModelCache(aiPreferences, notificationService, taskExecutor, mock(EmbeddingModelMetadataService.class));
    }

    @Test
    void getOrCreateReturnsSameInstanceForSameModelName() {
        AsyncEmbeddingModel first = cache.getOrCreate(MODEL_A);
        AsyncEmbeddingModel second = cache.getOrCreate(MODEL_A);

        assertSame(first, second);
    }

    @Test
    void getOrCreateReturnsDifferentInstanceForDifferentModelName() {
        AsyncEmbeddingModel modelA = cache.getOrCreate(MODEL_A);
        AsyncEmbeddingModel modelB = cache.getOrCreate(MODEL_B);

        assertNotSame(modelA, modelB);
    }

    @Test
    void closeDoesNotThrowAndClearsCache() {
        cache.getOrCreate(MODEL_A);
        cache.close();

        AsyncEmbeddingModel afterClose = cache.getOrCreate(MODEL_A);
        AsyncEmbeddingModel secondAfterClose = cache.getOrCreate(MODEL_A);

        assertSame(afterClose, secondAfterClose);
    }

    @Test
    void getOrCreateSchedulesModelInitializationWithTaskExecutor() {
        cache.getOrCreate(MODEL_A);

        verify(taskExecutor, atLeastOnce()).execute(any());
    }
}
