package org.jabref.logic.ai.embedding;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingModelMetadataServiceTest {

    private EmbeddingModelMetadataService service;

    @BeforeEach
    void setUp() {
        service = EmbeddingModelMetadataService.getInstance();
    }

    @Test
    void getAvailableModelsReturnsNonEmptyListWithDefaultModels() {
        List<String> models = service.getAvailableModels();

        assertFalse(models.isEmpty());
        assertTrue(models.contains("sentence-transformers/all-MiniLM-L12-v2"));
        assertTrue(models.contains("sentence-transformers/all-MiniLM-L6-v2"));
    }

    @Test
    void getMetadataReturnsEmptyForBlank() {
        assertTrue(service.getMetadata("").isEmpty());
        assertTrue(service.getMetadata("   ").isEmpty());
    }

    @Test
    void getMetadataResolvesDjlArtifactMetadata() {
        String modelName = "sentence-transformers/all-MiniLM-L6-v2";
        Optional<EmbeddingModelMetadata> metadataOpt = service.getMetadata(modelName);

        assertTrue(metadataOpt.isPresent());
        EmbeddingModelMetadata metadata = metadataOpt.get();
        assertEquals(modelName, metadata.modelName());
        assertTrue(metadata.downloadSizeBytes().isPresent());
        assertTrue(metadata.maxSnippetTokens().isPresent());
        assertEquals(256, metadata.maxSnippetTokens().orElseThrow());
    }

    @Test
    void getMetadataCachesResult() {
        String modelName = "sentence-transformers/all-MiniLM-L6-v2";
        Optional<EmbeddingModelMetadata> first = service.getMetadata(modelName);
        Optional<EmbeddingModelMetadata> second = service.getMetadata(modelName);

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertSame(first.get(), second.get());
    }

    @Test
    void metadataDisplayLabelFormatsCorrectly() {
        EmbeddingModelMetadata metadata = new EmbeddingModelMetadata(
                "test/model",
                OptionalLong.of(1024 * 1024 * 50),
                OptionalInt.of(512)
        );

        String label = metadata.displayLabel();
        assertTrue(label.contains("test/model"));
        assertTrue(label.contains("50 MB"));
        assertTrue(label.contains("512 tokens"));
    }
}
