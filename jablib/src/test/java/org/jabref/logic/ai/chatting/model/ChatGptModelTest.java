package org.jabref.logic.ai.chatting.model;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.model.ai.AiProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class ChatGptModelTest {
    
    @Mock
    private AiPreferences aiPreferences;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock basic preferences needed for ChatGPT model creation
        when(aiPreferences.getAiProvider()).thenReturn(AiProvider.OPEN_AI);
        when(aiPreferences.getSelectedChatModel()).thenReturn("gpt-4o-mini");
        when(aiPreferences.getTemperature()).thenReturn(0.7);
        when(aiPreferences.getSelectedApiBaseUrl()).thenReturn("https://api.openai.com/v1");
        when(aiPreferences.getApiKeyForAiProvider(AiProvider.OPEN_AI)).thenReturn("test-api-key");
    }
    
    @Test
    void chatGptModelCanBeCreated() {
        // Test that ChatGPT model can be instantiated without throwing exceptions
        ChatGptModel chatGptModel = new ChatGptModel(aiPreferences);
        assertNotNull(chatGptModel);
    }
    
    @Test
    void chatGptModelWorksWithCustomBaseUrl() {
        // Test that ChatGPT model works with custom OpenAI-compatible endpoints
        when(aiPreferences.getSelectedApiBaseUrl()).thenReturn("https://custom-api.example.com/v1");
        
        ChatGptModel chatGptModel = new ChatGptModel(aiPreferences);
        assertNotNull(chatGptModel);
    }
}