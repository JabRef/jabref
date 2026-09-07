package org.jabref.gui.preferences.ai;

import org.jabref.logic.ai.models.AiModelService;
import org.jabref.logic.ai.preferences.AiDefaultExpertSettings;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@NullMarked
class AiTabViewModelTest {

    private AiTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        AiPreferences aiPreferences = AiPreferences.getDefault();
        AiPreferences workingAiPreferences = AiPreferences.getDefault();
        AiModelService aiModelService = mock(AiModelService.class);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);

        viewModel = new AiTabViewModel(aiPreferences, workingAiPreferences, aiModelService, taskExecutor);
        viewModel.setValues();
    }

    @Test
    void documentSplitterChunkSizeValidWithinBounds() {
        viewModel.documentSplitterChunkSizeProperty().set(AiDefaultExpertSettings.DOCUMENT_SPLITTER_MAX_CHUNK_SIZE);
        assertTrue(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());

        viewModel.documentSplitterChunkSizeProperty().set(1);
        assertTrue(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());

        viewModel.documentSplitterChunkSizeProperty().set(300);
        assertTrue(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());
    }

    @Test
    void documentSplitterChunkSizeInvalidWhenZeroOrNegative() {
        viewModel.documentSplitterChunkSizeProperty().set(0);
        assertFalse(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());

        viewModel.documentSplitterChunkSizeProperty().set(-1);
        assertFalse(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());
    }

    @Test
    void documentSplitterChunkSizeInvalidWhenGreaterThanMaximum() {
        viewModel.documentSplitterChunkSizeProperty().set(AiDefaultExpertSettings.DOCUMENT_SPLITTER_MAX_CHUNK_SIZE + 1);
        assertFalse(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());

        viewModel.documentSplitterChunkSizeProperty().set(1000);
        assertFalse(viewModel.getDocumentSplitterChunkSizeValidationStatus().isValid());
    }
}
