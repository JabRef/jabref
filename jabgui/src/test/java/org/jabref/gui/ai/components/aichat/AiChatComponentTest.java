package org.jabref.gui.ai.components.aichat;

import java.util.Map;

import org.jabref.gui.preferences.ai.AiTabViewModel;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.ai.AiProvider;
import org.jabref.model.ai.EmbeddingModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AiTabViewModelTest {

    private AiPreferences prefs;

    private AiTabViewModel vm;

    @BeforeEach
    void setUp() {
        prefs = new AiPreferences(
                /* enableAi */ true,
                /* autoGenerateEmbeddings */ false,
                /* autoGenerateSummaries */ false,
                /* aiProvider */ AiProvider.OPEN_AI,
                /* openAiChatModel */ "gpt-4o",
                /* mistralAiChatModel */ "mistral-large",
                /* geminiChatModel */ "gemini-1.5-pro",
                /* huggingFaceChatModel */ "hf-model",
                /* gpt4AllModel */ "gpt4all",
                /* customizeExpertSettings */ true,
                /* openAiApiBaseUrl */ "https://api.openai.com/v1",
                /* mistralAiApiBaseUrl */ "https://api.mistral.ai",
                /* geminiApiBaseUrl */ "https://generativelanguage.googleapis.com",
                /* huggingFaceApiBaseUrl */ "https://api-inference.huggingface.co",
                /* gpt4AllApiBaseUrl */ "http://localhost:4891",
                /* embeddingModel */ EmbeddingModel.values()[0],
                /* temperature */ 0.7,
                /* contextWindowSize */ 8192,
                /* documentSplitterChunkSize */ 1000,
                /* documentSplitterOverlapSize */ 200,
                /* ragMaxResultsCount */ 5,
                /* ragMinScore */ 0.5,
                Map.of(
                        AiTemplate.CHATTING_SYSTEM_MESSAGE, "",
                        AiTemplate.CHATTING_USER_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE, "",
                        AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE, "",
                        AiTemplate.CITATION_PARSING_USER_MESSAGE, ""
                )
        );

        CliPreferences cliPrefs = Mockito.mock(CliPreferences.class, Mockito.RETURNS_DEEP_STUBS);
        when(cliPrefs.getAiPreferences()).thenReturn(prefs);

        vm = new AiTabViewModel(cliPrefs);
        vm.setValues();
    }

    @Test
    void setValues_populates_current_fields_from_prefs() {
        assertEquals(AiProvider.OPEN_AI, vm.selectedAiProviderProperty().get());
        assertEquals("gpt-4o", vm.selectedChatModelProperty().get());
        assertEquals("https://api.openai.com/v1", vm.apiBaseUrlProperty().get());
        assertTrue(vm.customizeExpertSettingsProperty().get());
        assertEquals(prefs.getEmbeddingModel(), vm.selectedEmbeddingModelProperty().get());
    }

    @Test
    void switching_provider_preserves_old_and_loads_new_current_fields() {
        // Change current OpenAI values first
        vm.selectedChatModelProperty().set("gpt-4o-mini");
        vm.apiKeyProperty().set("OPENAI_KEY_123");
        vm.apiBaseUrlProperty().set("https://api.openai.com/v99");

        // Switch to Mistral
        vm.selectedAiProviderProperty().set(AiProvider.MISTRAL_AI);

        // Current fields now reflect Mistral
        assertEquals(AiProvider.MISTRAL_AI, vm.selectedAiProviderProperty().get());
        assertEquals("https://api.mistral.ai", vm.apiBaseUrlProperty().get());

        // Switch back to OpenAI, we should get the previously edited values back
        vm.selectedAiProviderProperty().set(AiProvider.OPEN_AI);
        assertEquals("gpt-4o-mini", vm.selectedChatModelProperty().get());
        assertEquals("https://api.openai.com/v99", vm.apiBaseUrlProperty().get());
    }
}
