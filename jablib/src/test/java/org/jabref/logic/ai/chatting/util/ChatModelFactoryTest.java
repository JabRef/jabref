package org.jabref.logic.ai.chatting.util;

import java.util.List;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    void createDebugChatModel() {
        // [utest->req~ai.llms.providers.debug~1]
        try (ChatModel chatModel = ChatModelFactory.create(
                AiProvider.DEBUG,
                "debug",
                "",
                0.7,
                "",
                128000,
                TokenEstimatorKind.WORDS
        )) {
            assertNotNull(chatModel);
            assertEquals(AiProvider.DEBUG, chatModel.getAiProvider());
            assertEquals("debug", chatModel.getName());
            assertEquals(128000, chatModel.getContextWindowSize());

            ChatResponse response = chatModel.chat(List.of(
                    new SystemMessage("test system"),
                    new UserMessage("test user")
            ));

            String expected = """
                    Hi! I am a dummy AI provider, I am made to test and debug the AI features.

                    I have this system message:
                    test system

                    And I received this user message:
                    test user""";

            assertEquals(expected, response.aiMessage().text());
        }
    }

    @Test
    void createDebugChatModelFromPreferences() {
        // [utest->req~ai.llms.providers.debug~1]
        AiPreferences preferences = AiPreferences.getDefault();
        preferences.setCustomizeExpertSettings(true);
        preferences.setEnableAiDebugProvider(true);

        try (ChatModel chatModel = ChatModelFactory.create(preferences)) {
            assertNotNull(chatModel);
            assertEquals(AiProvider.DEBUG, chatModel.getAiProvider());
            assertEquals("debug", chatModel.getName());
        }
    }

    @Test
    void debugProviderIgnoredWhenExpertSettingsDisabled() {
        // [utest->req~ai.llms.providers.debug~1]
        AiPreferences preferences = AiPreferences.getDefault();
        preferences.setCustomizeExpertSettings(false);
        preferences.setEnableAiDebugProvider(true);

        try (ChatModel chatModel = ChatModelFactory.create(preferences)) {
            assertNotNull(chatModel);
            assertEquals(AiProvider.OPEN_AI, chatModel.getAiProvider());
        }
    }
}
