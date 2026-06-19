package org.jabref.model.ai.llm;

public enum PredefinedChatModel {
    GPT_4O_MINI(AiProvider.OPEN_AI, "gpt-4o-mini", 128000),
    GPT_4O(AiProvider.OPEN_AI, "gpt-4o", 128000),
    GPT_4(AiProvider.OPEN_AI, "gpt-4", 8192),
    GPT_4_TURBO(AiProvider.OPEN_AI, "gpt-4-turbo", 128000),
    GPT_3_5_TURBO(AiProvider.OPEN_AI, "gpt-3.5-turbo", 16385),
    OPEN_MISTRAL_NEMO(AiProvider.MISTRAL_AI, "open-mistral-nemo", 128000),
    OPEN_MISTRAL_7B(AiProvider.MISTRAL_AI, "open-mistral-7b", 32000),
    // "mixtral" is not a typo.
    OPEN_MIXTRAL_8X7B(AiProvider.MISTRAL_AI, "open-mixtral-8x7b", 32000),
    OPEN_MIXTRAL_8X22B(AiProvider.MISTRAL_AI, "open-mixtral-8x22b", 64000),
    GEMINI_1_5_FLASH(AiProvider.GEMINI, "gemini-1.5-flash", 1048576),
    GEMINI_1_5_PRO(AiProvider.GEMINI, "gemini-1.5-pro", 2097152),
    GEMINI_1_0_PRO(AiProvider.GEMINI, "gemini-1.0-pro", 32000),
    // Dummy variant for Hugging Face models.
    // Blank entry is used for cases where the model name is not specified.
    BLANK_HUGGING_FACE(AiProvider.HUGGING_FACE, "", 0);

    private final AiProvider aiProvider;
    private final String name;
    private final int contextWindowSize;

    PredefinedChatModel(AiProvider aiProvider, String name, int contextWindowSize) {
        this.aiProvider = aiProvider;
        this.name = name;
        this.contextWindowSize = contextWindowSize;
    }

    public AiProvider getAiProvider() {
        return aiProvider;
    }

    public String getName() {
        return name;
    }

    public int getContextWindowSize() {
        return contextWindowSize;
    }

    public String toString() {
        return aiProvider.toString() + " " + name;
    }
}
