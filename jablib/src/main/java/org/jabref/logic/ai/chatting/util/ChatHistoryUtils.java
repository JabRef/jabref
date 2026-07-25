package org.jabref.logic.ai.chatting.util;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.ai.chatting.ChatMessage;

public final class ChatHistoryUtils {
    private ChatHistoryUtils() {
        throw new UnsupportedOperationException("unable to instantiate a utility class");
    }

    /// Removes the message with the specified ID from history.
    /// Leaves a "hole" in context, but this is intended.
    public static void delete(List<ChatMessage> chatHistory, String id) {
        chatHistory.removeIf(message -> Objects.equals(message.id(), id));
    }

    /// Rewinds history to the point before the specified message and returns the user content to be re-sent.
    ///
    /// Modifies the chat history in-place.
    ///
    /// @return the content to regenerate or empty if the message was not found
    public static Optional<String> regenerate(List<ChatMessage> chatHistory, String id) {
        Optional<ChatMessage> recordOpt = chatHistory
                .stream()
                .filter(message -> Objects.equals(message.id(), id))
                .findFirst();

        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        ChatMessage message = recordOpt.get();
        String contentToRegenerate = message.content();
        Instant cutoffTime = message.timestamp();

        if (message.role() != ChatMessage.Role.USER) {
            int index = chatHistory.indexOf(message);
            if (index > 0) {
                ChatMessage prev = chatHistory.get(index - 1);
                if (prev.role() == ChatMessage.Role.USER) {
                    contentToRegenerate = prev.content();
                    cutoffTime = prev.timestamp();
                }
            }
        }

        final Instant finalCutoffTime = cutoffTime;
        chatHistory.removeIf(historyMessage ->
                !historyMessage.timestamp().isBefore(finalCutoffTime)
        );

        return Optional.of(contentToRegenerate);
    }

    /// Updates the system message in the chat history.
    /// If a system message already exists, it is replaced. Otherwise, a new system message is added at the beginning.
    ///
    /// @param chatHistory      the chat history to update
    /// @param newSystemMessage the new system message content
    public static void updateSystemMessage(List<ChatMessage> chatHistory, String newSystemMessage) {
        // Remove existing system message if present
        chatHistory.removeIf(message -> message.role() == ChatMessage.Role.SYSTEM);

        // Add new system message at the beginning
        if (newSystemMessage != null && !newSystemMessage.isEmpty()) {
            chatHistory.addFirst(ChatMessage.systemMessage(newSystemMessage));
        }
    }

    /// Finds the last user message in the chat history by iterating backwards.
    ///
    /// @param chatHistory the chat history to search
    /// @return the last user messag, or empty if no user message is found
    public static Optional<ChatMessage> getLastUserMessage(List<ChatMessage> chatHistory) {
        for (int i = chatHistory.size() - 1; i >= 0; i--) {
            if (chatHistory.get(i).role() == ChatMessage.Role.USER) {
                return Optional.of(chatHistory.get(i));
            }
        }
        return Optional.empty();
    }
}
