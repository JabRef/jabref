package org.jabref.model.ai.pipeline;

import org.jabref.model.ai.AiDefaultEnums;

public enum AnswerEngineKind {
    EMBEDDINGS_SEARCH,
    FULL_DOCUMENT;

    public static AnswerEngineKind safeValueOf(String name) {
        try {
            return AnswerEngineKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.ANSWER_ENGINE_KIND;
        }
    }
}
