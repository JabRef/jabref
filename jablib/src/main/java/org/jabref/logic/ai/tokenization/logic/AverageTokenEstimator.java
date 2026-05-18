package org.jabref.logic.ai.tokenization.logic;

import java.util.List;

import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public class AverageTokenEstimator implements TokenEstimator {
    private final ByCharacterTokenEstimator byCharacterTokenizer = new ByCharacterTokenEstimator();
    private final ByWordsTokenEstimator byWordsTokenizer = new ByWordsTokenEstimator();

    @Override
    public int estimate(ChatMessage.Role role, String content) {
        int byCharacter = byCharacterTokenizer.estimate(role, content);
        int byWords = byWordsTokenizer.estimate(role, content);

        return calculate(byCharacter, byWords);
    }

    @Override
    public int estimate(List<ChatMessage> messages) {
        int byCharacter = byCharacterTokenizer.estimate(messages);
        int byWords = byWordsTokenizer.estimate(messages);

        return calculate(byCharacter, byWords);
    }

    private int calculate(int byCharacter, int byWords) {
        return Math.round((byWords + byCharacter) / 2.0f);
    }

    @Override
    public TokenEstimatorKind getKind() {
        return TokenEstimatorKind.AVERAGE;
    }
}
