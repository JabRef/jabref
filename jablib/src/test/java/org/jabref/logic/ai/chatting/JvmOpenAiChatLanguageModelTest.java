package org.jabref.logic.ai.chatting;

import java.net.http.HttpClient;
import java.util.List;

import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmOpenAiChatLanguageModelTest {

    @Test
    void connectionFailureNamesBaseUrl() {
        String baseUrl = "http://localhost:1/v1/";
        JvmOpenAiChatLanguageModel model = new JvmOpenAiChatLanguageModel("key", "model", 0.7, baseUrl, HttpClient.newHttpClient());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> model.chat(List.of(UserMessage.from("hi"))));

        assertTrue(exception.getMessage().contains(baseUrl), exception.getMessage());
    }
}
