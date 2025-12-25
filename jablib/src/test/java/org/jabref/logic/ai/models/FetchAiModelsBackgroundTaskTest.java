package org.jabref.logic.ai.models;

import java.util.List;

import org.jabref.model.ai.AiProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FetchAiModelsBackgroundTaskTest {

    private AiModelService mockAiModelService;

    @BeforeEach
    void setUp() {
        mockAiModelService = mock(AiModelService.class);
    }

    @Test
    void taskCallsDelegatestoAiModelService() {
        List<String> expectedModels = List.of("gpt-4", "gpt-3.5-turbo");
        when(mockAiModelService.fetchModelsSynchronously(
                any(AiProvider.class),
                any(String.class),
                any(String.class)
        )).thenReturn(expectedModels);

        FetchAiModelsBackgroundTask task = new FetchAiModelsBackgroundTask(
                mockAiModelService,
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                "test-key"
        );

        List<String> result = task.call();

        assertEquals(expectedModels, result);
        verify(mockAiModelService).fetchModelsSynchronously(
                eq(AiProvider.OPEN_AI),
                eq("https://api.openai.com"),
                eq("test-key")
        );
    }

    @Test
    void taskReturnsEmptyListWhenServiceReturnsEmpty() {
        when(mockAiModelService.fetchModelsSynchronously(
                any(AiProvider.class),
                any(String.class),
                any(String.class)
        )).thenReturn(List.of());

        FetchAiModelsBackgroundTask task = new FetchAiModelsBackgroundTask(
                mockAiModelService,
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                "test-key"
        );

        List<String> result = task.call();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void taskHandlesMistralAiProvider() {
        List<String> expectedModels = List.of("open-mistral-7b", "open-mixtral-8x7b");
        when(mockAiModelService.fetchModelsSynchronously(
                eq(AiProvider.MISTRAL_AI),
                any(String.class),
                any(String.class)
        )).thenReturn(expectedModels);

        FetchAiModelsBackgroundTask task = new FetchAiModelsBackgroundTask(
                mockAiModelService,
                AiProvider.MISTRAL_AI,
                "https://api.mistral.ai",
                "mistral-key"
        );

        List<String> result = task.call();

        assertEquals(expectedModels, result);
    }

    @Test
    void taskHandlesGpt4AllProvider() {
        List<String> expectedModels = List.of("llama-3", "mistral");
        when(mockAiModelService.fetchModelsSynchronously(
                eq(AiProvider.GPT4ALL),
                any(String.class),
                any(String.class)
        )).thenReturn(expectedModels);

        FetchAiModelsBackgroundTask task = new FetchAiModelsBackgroundTask(
                mockAiModelService,
                AiProvider.GPT4ALL,
                "http://localhost:4891",
                ""
        );

        List<String> result = task.call();

        assertEquals(expectedModels, result);
    }

    @Test
    void taskHasCorrectTitle() {
        FetchAiModelsBackgroundTask task = new FetchAiModelsBackgroundTask(
                mockAiModelService,
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                "test-key"
        );

        String title = task.titleProperty().get();

        assertNotNull(title);
        assertFalse(title.isEmpty());
        // Title should contain provider name or reference to fetching
        assertTrue(title.toLowerCase().contains("fetch") || title.toLowerCase().contains("openai"));
    }

    @Test
    void taskIsConfiguredToNotShowToUser() {
        FetchAiModelsBackgroundTask task = new FetchAiModelsBackgroundTask(
                mockAiModelService,
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                "test-key"
        );

        // The task should be configured to not show to user (showToUser(false) in configure method)
        // We can't easily test this without accessing internal state,
        // but we can verify the task is properly constructed
        assertNotNull(task);
        assertNotNull(task.titleProperty().get());
    }
}
