package org.jabref.logic.ai.tokenization.logic;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public class ByCharacterTokenEstimator implements TokenEstimator {
    private static final float CHAR_FACTOR = 4;

    @Override
    public int estimate(ChatMessage.Role role, String content) {
        return calculate(content);
    }

    @Override
    public int estimate(List<ChatMessage> messages) {
        String content = messages
                .stream()
                .map(ChatMessage::content)
                .collect(Collectors.joining(" "));

        return calculate(content);
    }

    private int calculate(String content) {
        return Math.round(content.length() / CHAR_FACTOR);
    }

    @Override
    public TokenEstimatorKind getKind() {
        return TokenEstimatorKind.CHARS;
    }
}
