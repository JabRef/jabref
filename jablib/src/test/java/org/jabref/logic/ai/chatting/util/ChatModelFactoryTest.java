package org.jabref.logic.ai.chatting.util;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class ChatModelFactoryTest {

    /// Building a model must not fail on module-path service loading (e.g., a missing `uses` for Mistral AI's builder factory)
    @ParameterizedTest
    @EnumSource(AiProvider.class)
    void createDoesNotThrow(AiProvider provider) {
        try (ChatModel model = ChatModelFactory.create(provider, "model", "key", 0.7, provider.getApiUrl(), 4096, TokenEstimatorKind.AVERAGE)) {
            assertEquals("model", model.getName());
        }
    }
}
