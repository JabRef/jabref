package org.jabref.logic.ai.embedding;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.preferences.AiPreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class EmbeddingModelMetadataServiceTest {

    private EmbeddingModelMetadataService service;

    @BeforeEach
    void setUp() {
        AiPreferences preferences = AiPreferences.getDefault();
        preferences.setAiFeaturesEnabledCurrently(true);
        service = new EmbeddingModelMetadataService(preferences);
    }

    @Test
    void getAvailableModelsReturnsNonEmptyList() {
        List<String> models = service.getAvailableModels();

        assertFalse(models.isEmpty());
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
    void getMetadataReturnsEmptyWhenAiDisabled() {
        AiPreferences preferences = AiPreferences.getDefault();
        preferences.setAiFeaturesEnabledCurrently(false);
        EmbeddingModelMetadataService disabledService = new EmbeddingModelMetadataService(preferences);

        assertTrue(disabledService.getMetadata("sentence-transformers/all-MiniLM-L6-v2").isEmpty());
    }
}
