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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@NullMarked
class ChatModelFactoryTest {

    @Test
    void createDummyChatModel() {
        try (ChatModel chatModel = ChatModelFactory.create(
                AiProvider.DUMMY,
                "dummy",
                "",
                0.7,
                "",
                128000,
                TokenEstimatorKind.WORDS
        )) {
            assertNotNull(chatModel);
            assertEquals(AiProvider.DUMMY, chatModel.getAiProvider());
            assertEquals("dummy", chatModel.getName());
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
    void createDummyChatModelFromPreferences() {
        AiPreferences preferences = AiPreferences.getDefault();
        preferences.setAiProvider(AiProvider.DUMMY);

        try (ChatModel chatModel = ChatModelFactory.create(preferences)) {
            assertNotNull(chatModel);
            assertEquals(AiProvider.DUMMY, chatModel.getAiProvider());
            assertEquals("dummy", chatModel.getName());
        }
    }
}
