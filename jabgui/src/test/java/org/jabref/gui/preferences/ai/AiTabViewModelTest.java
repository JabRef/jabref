package org.jabref.gui.preferences.ai;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.jabref.logic.ai.embedding.EmbeddingModelMetadata;
import org.jabref.logic.ai.embedding.EmbeddingModelMetadataService;
import org.jabref.logic.ai.models.AiModelService;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class AiTabViewModelTest {

    private EmbeddingModelMetadataService embeddingModelMetadataService;
    private AiModelService aiModelService;
    private AiTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        embeddingModelMetadataService = mock(EmbeddingModelMetadataService.class);
        aiModelService = mock(AiModelService.class);

        when(embeddingModelMetadataService.getMetadata("test-model")).thenReturn(
                Optional.of(new EmbeddingModelMetadata("test-model", OptionalLong.of(1024), OptionalInt.of(256)))
        );

        AiPreferences aiPreferences = AiPreferences.getDefault();
        AiPreferences workingAiPreferences = AiPreferences.getDefault();

        viewModel = new AiTabViewModel(
                aiPreferences,
                workingAiPreferences,
                aiModelService,
                new CurrentThreadTaskExecutor(),
                embeddingModelMetadataService
        );
    }

    @Test
    void maxChunkSizeLabelUpdatesWhenModelSelected() {
        viewModel.selectedEmbeddingModelProperty().set("test-model");

        assertEquals(256, viewModel.selectedEmbeddingModelMaxChunkSizeProperty().get());
    }

    @Test
    void maxChunkSizeFallsBackToDefaultWhenUnknown() {
        when(embeddingModelMetadataService.getMetadata("unknown-model")).thenReturn(
                Optional.of(new EmbeddingModelMetadata("unknown-model", OptionalLong.empty(), OptionalInt.empty()))
        );

        viewModel.selectedEmbeddingModelProperty().set("unknown-model");

        assertEquals(512, viewModel.selectedEmbeddingModelMaxChunkSizeProperty().get());
    }

    @Test
    void documentSplitterChunkSizeValidWithinMaxSnippetTokens() {
        viewModel.selectedEmbeddingModelProperty().set("test-model");
        viewModel.documentSplitterChunkSizeProperty().set(200);

        assertTrue(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());
    }

    @Test
    void documentSplitterChunkSizeInvalidWhenExceedingMaxSnippetTokens() {
        viewModel.selectedEmbeddingModelProperty().set("test-model");
        viewModel.documentSplitterChunkSizeProperty().set(300);

        assertFalse(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());
    }
}
