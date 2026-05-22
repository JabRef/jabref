package org.jabref.logic.ai.preferences;

import java.util.Map;

import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.llm.PredefinedChatModel;

public class AiProviderDefaultChatModels {
    private static final Map<AiProvider, PredefinedChatModel> CHAT_MODELS = Map.of(
            AiProvider.OPEN_AI, PredefinedChatModel.GPT_4O_MINI,
            AiProvider.MISTRAL_AI, PredefinedChatModel.OPEN_MIXTRAL_8X22B,
            AiProvider.GEMINI, PredefinedChatModel.GEMINI_1_5_FLASH,
            AiProvider.HUGGING_FACE, PredefinedChatModel.BLANK_HUGGING_FACE
    );

    public static PredefinedChatModel getDefaultChatModel(AiProvider aiProvider) {
        return CHAT_MODELS.get(aiProvider);
    }
}
