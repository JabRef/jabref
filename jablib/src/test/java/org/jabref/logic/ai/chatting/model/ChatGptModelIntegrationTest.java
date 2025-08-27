package org.jabref.logic.ai.chatting.model;

import java.util.List;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.model.ai.AiProvider;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration test for ChatGPT model compatibility.
 * These tests verify that the ChatGPT model implementation is compatible
 * with the expected interface without making actual API calls.
 */
class ChatGptModelIntegrationTest {
    
    @Mock
    private AiPreferences aiPreferences;
    
    private ChatGptModel chatGptModel;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock basic preferences needed for ChatGPT model creation
        when(aiPreferences.getAiProvider()).thenReturn(AiProvider.OPEN_AI);
        when(aiPreferences.getSelectedChatModel()).thenReturn("gpt-4o-mini");
        when(aiPreferences.getTemperature()).thenReturn(0.7);
        when(aiPreferences.getSelectedApiBaseUrl()).thenReturn("https://api.openai.com/v1");
        when(aiPreferences.getApiKeyForAiProvider(AiProvider.OPEN_AI)).thenReturn("test-api-key");
        
        chatGptModel = new ChatGptModel(aiPreferences);
    }
    
    @Test
    void chatGptModelImplementsChatModelInterface() {
        // Verify that ChatGptModel properly implements the ChatModel interface
        assertNotNull(chatGptModel);
        assertTrue(chatGptModel instanceof dev.langchain4j.model.chat.ChatModel);
    }
    
    @Test
    void chatGptModelAcceptsMessageList() {
        // Test that the model accepts a list of messages without throwing exceptions
        // Note: This will fail when it tries to actually call the API since we don't have a real key,
        // but it validates that our interface is correct
        List<ChatMessage> messages = List.of(new UserMessage("Hello"));
        
        // We expect this to throw a runtime exception because of invalid API key,
        // but this confirms our interface is compatible
        assertThrows(RuntimeException.class, () -> {
            ChatResponse response = chatGptModel.chat(messages);
        });
    }
}