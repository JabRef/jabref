package org.jabref.logic.ai.chatting.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.ai.chatting.ChatMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->req~ai.chat.delete-messages~1]
// [utest->req~ai.chat.regenerate-response~1]
// [utest->req~ai.chat.clear-history~1]
class ChatHistoryUtilsTest {

    @Test
    void deleteRemovesMessageWithMatchingId() {
        ChatMessage msg = ChatMessage.userMessage("to delete");
        List<ChatMessage> history = new ArrayList<>(List.of(
                ChatMessage.userMessage("keep"),
                msg
        ));

        ChatHistoryUtils.delete(history, msg.id());

        assertEquals(1, history.size());
        assertEquals("keep", history.getFirst().content());
    }

    @Test
    void deleteDoesNothingWhenIdNotFound() {
        List<ChatMessage> history = new ArrayList<>(List.of(
                ChatMessage.userMessage("keep")
        ));

        ChatHistoryUtils.delete(history, "non-existent-id");

        assertEquals(1, history.size());
    }

    @Test
    void regenerateOnUserMessageReturnsItsContentAndTruncatesHistory() {
        ChatMessage user = new ChatMessage("id-user", Instant.ofEpochMilli(1000), ChatMessage.Role.USER, "user question", List.of());
        ChatMessage ai = new ChatMessage("id-ai", Instant.ofEpochMilli(2000), ChatMessage.Role.AI, "ai answer", List.of());
        List<ChatMessage> history = new ArrayList<>(List.of(user, ai));

        Optional<String> toRegenerate = ChatHistoryUtils.regenerate(history, user.id());

        assertTrue(toRegenerate.isPresent());
        assertEquals("user question", toRegenerate.get());
        // Messages at or after the user message timestamp are removed
        assertTrue(history.stream().allMatch(m -> m.timestamp().isBefore(user.timestamp())));
    }

    @Test
    void regenerateOnAiMessageReturnsAssociatedUserContent() {
        ChatMessage user = new ChatMessage("id-user", Instant.ofEpochMilli(1000), ChatMessage.Role.USER, "original question", List.of());
        ChatMessage ai = new ChatMessage("id-ai", Instant.ofEpochMilli(2000), ChatMessage.Role.AI, "ai answer", List.of());
        List<ChatMessage> history = new ArrayList<>(List.of(user, ai));

        Optional<String> toRegenerate = ChatHistoryUtils.regenerate(history, ai.id());

        assertTrue(toRegenerate.isPresent());
        assertEquals("original question", toRegenerate.get());
    }

    @Test
    void regenerateReturnsNullWhenIdNotFound() {
        List<ChatMessage> history = new ArrayList<>(List.of(
                ChatMessage.userMessage("question")
        ));

        Optional<String> result = ChatHistoryUtils.regenerate(history, "ghost-id");

        assertTrue(result.isEmpty());
    }

    @Test
    void updateSystemMessageReplacesExistingSystemMessage() {
        ChatMessage existing = ChatMessage.systemMessage("old system");
        List<ChatMessage> history = new ArrayList<>(List.of(
                existing,
                ChatMessage.userMessage("hello")
        ));

        ChatHistoryUtils.updateSystemMessage(history, "new system");

        long systemCount = history.stream().filter(m -> m.role() == ChatMessage.Role.SYSTEM).count();
        assertEquals(1, systemCount);
        assertEquals("new system", history.getFirst().content());
    }

    @Test
    void updateSystemMessageInsertsAtBeginningWhenNonePresent() {
        List<ChatMessage> history = new ArrayList<>(List.of(
                ChatMessage.userMessage("hello")
        ));

        ChatHistoryUtils.updateSystemMessage(history, "system prompt");

        assertEquals(ChatMessage.Role.SYSTEM, history.getFirst().role());
        assertEquals("system prompt", history.getFirst().content());
        assertEquals(2, history.size());
    }

    @Test
    void updateSystemMessageDoesNotAddWhenNewMessageIsEmpty() {
        List<ChatMessage> history = new ArrayList<>(List.of(
                ChatMessage.systemMessage("old system"),
                ChatMessage.userMessage("hello")
        ));

        ChatHistoryUtils.updateSystemMessage(history, "");

        long systemCount = history.stream().filter(m -> m.role() == ChatMessage.Role.SYSTEM).count();
        assertEquals(0, systemCount);
    }

    @Test
    void getLastUserMessageReturnsLastUser() {
        ChatMessage user1 = ChatMessage.userMessage("first question");
        ChatMessage ai = ChatMessage.aiMessage("answer", List.of());
        ChatMessage user2 = ChatMessage.userMessage("second question");
        List<ChatMessage> history = List.of(user1, ai, user2);

        Optional<ChatMessage> last = ChatHistoryUtils.getLastUserMessage(history);

        assertTrue(last.isPresent());
        assertEquals("second question", last.get().content());
    }

    @Test
    void getLastUserMessageReturnsNullWhenNoUserMessage() {
        List<ChatMessage> history = List.of(ChatMessage.aiMessage("only AI", List.of()));

        Optional<ChatMessage> last = ChatHistoryUtils.getLastUserMessage(history);

        assertTrue(last.isEmpty());
    }

    @Test
    void getLastUserMessageReturnsNullForEmptyHistory() {
        Optional<ChatMessage> last = ChatHistoryUtils.getLastUserMessage(List.of());

        assertTrue(last.isEmpty());
    }
}
