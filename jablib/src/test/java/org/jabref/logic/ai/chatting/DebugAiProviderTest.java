package org.jabref.logic.ai.chatting;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@NullMarked
class DebugAiProviderTest {

    private DebugAiProvider model;

    @BeforeEach
    void setUp() {
        model = new DebugAiProvider(0);
    }

    @Test
    void chatReturnsExpectedResponseWithSystemAndUserMessage() {
        // [utest->req~ai.llms.providers.debug~1]
        List<ChatMessage> messages = List.of(
                new SystemMessage("You are a helpful research assistant."),
                new UserMessage("Summarize the methodology.")
        );

        ChatResponse response = model.chat(messages);

        String expected = """
                Hi! I am a dummy AI provider, I am made to test and debug the AI features.

                I have this system message:
                You are a helpful research assistant.

                And I received this user message:
                Summarize the methodology.""";

        assertEquals(expected, response.aiMessage().text());
        assertEquals(FinishReason.STOP, response.finishReason());
        assertNotNull(response.tokenUsage());
    }

    @Test
    void chatReturnsExpectedResponseWithoutSystemMessage() {
        List<ChatMessage> messages = List.of(
                new UserMessage("Generate 3 follow-up questions.")
        );

        ChatResponse response = model.chat(messages);

        String expected = "Hi! I am a dummy AI provider, I am made to test and debug the AI features.\n\n"
                + "I have this system message:\n\n\n"
                + "And I received this user message:\n"
                + "Generate 3 follow-up questions.";

        assertEquals(expected, response.aiMessage().text());
    }

    @Test
    void chatUsesLastUserMessageWhenHistoryPresent() {
        List<ChatMessage> messages = List.of(
                new SystemMessage("System prompt"),
                new UserMessage("First user message"),
                new AiMessage("First AI response"),
                new UserMessage("Second user message really sent to AI")
        );

        ChatResponse response = model.chat(messages);

        String expected = """
                Hi! I am a dummy AI provider, I am made to test and debug the AI features.

                I have this system message:
                System prompt

                And I received this user message:
                Second user message really sent to AI""";

        assertEquals(expected, response.aiMessage().text());
    }

    @Test
    void defaultConstructorHasFiveSecondsDelay() {
        // [utest->req~ai.llms.providers.debug~1]
        DebugAiProvider defaultModel = new DebugAiProvider();
        assertEquals(5000, defaultModel.getDelayMs());
    }

    @Test
    void chatInvokesSleeperWithConfiguredDelay() {
        // [utest->req~ai.llms.providers.debug~1]
        AtomicLong sleptTime = new AtomicLong();
        DebugAiProvider debugModel = new DebugAiProvider(5000, sleptTime::set);
        debugModel.chat(List.of(new UserMessage("Hello")));
        assertEquals(5000, sleptTime.get());
    }
}
