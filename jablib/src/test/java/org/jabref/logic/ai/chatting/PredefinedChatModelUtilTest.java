package org.jabref.logic.ai.chatting;

import org.jabref.model.ai.llm.AiProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredefinedChatModelUtilTest {
    @Test
    void knownModelUsesItsContextWindowSize() {
        assertEquals(128000, PredefinedChatModelUtil.getContextWindowSize(AiProvider.OPEN_AI, "gpt-4o-mini"));
    }

    @Test
    void unknownModelUses64k() {
        assertEquals(65536, PredefinedChatModelUtil.getContextWindowSize(AiProvider.OPEN_AI, "granite4.2:8b"));
    }
}
