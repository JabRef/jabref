package org.jabref.logic.ai.tokenization.util;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.tokenization.logic.AverageTokenEstimator;
import org.jabref.logic.ai.tokenization.logic.ByCharacterTokenEstimator;
import org.jabref.logic.ai.tokenization.logic.ByWordsTokenEstimator;
import org.jabref.logic.ai.tokenization.logic.MaximumTokenEstimator;
import org.jabref.logic.ai.tokenization.logic.MinimumTokenEstimator;
import org.jabref.logic.ai.tokenization.logic.TokenEstimator;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public final class TokenEstimatorFactory {
    private TokenEstimatorFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static TokenEstimator create(TokenEstimatorKind kind) {
        return switch (kind) {
            case AVERAGE ->
                    new AverageTokenEstimator();
            case MAX ->
                    new MaximumTokenEstimator();
            case MIN ->
                    new MinimumTokenEstimator();
            case CHARS ->
                    new ByCharacterTokenEstimator();
            case WORDS ->
                    new ByWordsTokenEstimator();
        };
    }

    /// Convenience overload that reads the kind from {@link AiPreferences}.
    public static TokenEstimator create(AiPreferences aiPreferences) {
        return create(aiPreferences.getTokenEstimatorKind());
    }
}
