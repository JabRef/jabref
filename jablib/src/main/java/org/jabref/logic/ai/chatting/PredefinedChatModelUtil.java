package org.jabref.logic.ai.chatting;

import java.util.Arrays;
import java.util.List;

import org.jabref.logic.ai.preferences.AiDefaultExpertSettings;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.llm.PredefinedChatModel;

public class PredefinedChatModelUtil {
    public static List<String> getAvailableModels(AiProvider aiProvider) {
        return Arrays.stream(PredefinedChatModel.values())
                     .filter(model -> model.getAiProvider() == aiProvider)
                     .map(PredefinedChatModel::getName)
                     .toList();
    }

    public static int getContextWindowSize(AiProvider aiProvider, String modelName) {
        return Arrays.stream(PredefinedChatModel.values())
                     .filter(model -> model.getAiProvider() == aiProvider && model.getName().equals(modelName))
                     .map(PredefinedChatModel::getContextWindowSize)
                     .findFirst()
                     .orElse(AiDefaultExpertSettings.CONTEXT_WINDOW_SIZE);
    }
}
