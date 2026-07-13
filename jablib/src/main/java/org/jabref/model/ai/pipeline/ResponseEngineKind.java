package org.jabref.model.ai.pipeline;

import org.jabref.model.ai.AiDefaultEnums;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum ResponseEngineKind {
    EMBEDDINGS_SEARCH,
    FULL_DOCUMENT;

    public static ResponseEngineKind safeValueOf(String name) {
        try {
            return ResponseEngineKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.RESPONSE_ENGINE_KIND;
        }
    }
}

