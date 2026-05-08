package org.jabref.model.ai.pipeline;

import org.jabref.model.ai.AiDefaultEnums;

public enum DocumentSplitterKind {
    SLIDING_WINDOW;

    public static DocumentSplitterKind safeValueOf(String name) {
        try {
            return DocumentSplitterKind.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.DOCUMENT_SPLITTER_KIND;
        }
    }
}
