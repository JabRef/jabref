package org.jabref.logic.ai.tokenization.logic;

import java.util.List;

import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public class MaximumTokenEstimator implements TokenEstimator {
    private final ByCharacterTokenEstimator byCharacterTokenizer = new ByCharacterTokenEstimator();
    private final ByWordsTokenEstimator byWordsTokenizer = new ByWordsTokenEstimator();

    @Override
    public int estimate(ChatMessage.Role role, String content) {
        int byWords = byWordsTokenizer.estimate(role, content);
        int byCharacter = byCharacterTokenizer.estimate(role, content);

        return calculate(byWords, byCharacter);
    }

    @Override
    public int estimate(List<ChatMessage> messages) {
        int byWords = byWordsTokenizer.estimate(messages);
        int byCharacter = byCharacterTokenizer.estimate(messages);

        return calculate(byWords, byCharacter);
    }

    private int calculate(int byWords, int byCharacters) {
        return Math.max(byWords, byCharacters);
    }

    @Override
    public TokenEstimatorKind getKind() {
        return TokenEstimatorKind.MAX;
    }
}
