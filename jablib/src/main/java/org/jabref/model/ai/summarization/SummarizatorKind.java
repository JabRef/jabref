package org.jabref.model.ai.summarization;

import org.jabref.model.ai.AiDefaultEnums;

public enum SummarizatorKind {
    CHUNKED,
    FULL_DOCUMENT;

    public static SummarizatorKind safeValueOf(String name) {
        try {
            return SummarizatorKind.valueOf(name);
        } catch (IllegalArgumentException _) {
            return AiDefaultEnums.SUMMARIZATOR_KIND;
        }
    }
}
