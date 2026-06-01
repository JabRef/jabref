package org.jabref.logic.ai.embedding;

import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingModelCacheTest {

    private EmbeddingModelCache cache;

    @BeforeEach
    void setUp() {
        NotificationService notificationService = mock(NotificationService.class);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        when(taskExecutor.execute(any())).thenReturn(mock(java.util.concurrent.Future.class));

        cache = new EmbeddingModelCache(notificationService, taskExecutor);
    }

    @Test
    void getOrCreateReturnsSameInstanceForSameKind() {
        AsyncEmbeddingModel first = cache.getOrCreate(PredefinedEmbeddingModel.BAAI_BGE_SMALL_EN_V1_5);
        AsyncEmbeddingModel second = cache.getOrCreate(PredefinedEmbeddingModel.BAAI_BGE_SMALL_EN_V1_5);

        assertSame(first, second);
    }

    @Test
    void getOrCreateReturnsDifferentInstanceForDifferentKind() {
        AsyncEmbeddingModel modelA = cache.getOrCreate(PredefinedEmbeddingModel.BAAI_BGE_SMALL_EN_V1_5);
        AsyncEmbeddingModel modelB = cache.getOrCreate(PredefinedEmbeddingModel.INTFLOAT_E5_SMALL_V2);

        assertNotSame(modelA, modelB);
    }

    @Test
    void closeDoesNotThrowAndClearsCache() {
        cache.getOrCreate(PredefinedEmbeddingModel.BAAI_BGE_SMALL_EN_V1_5);
        cache.close();

        AsyncEmbeddingModel afterClose = cache.getOrCreate(PredefinedEmbeddingModel.BAAI_BGE_SMALL_EN_V1_5);
        AsyncEmbeddingModel secondAfterClose = cache.getOrCreate(PredefinedEmbeddingModel.BAAI_BGE_SMALL_EN_V1_5);

        assertSame(afterClose, secondAfterClose);
    }
}
