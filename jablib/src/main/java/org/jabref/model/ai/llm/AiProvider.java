package org.jabref.model.ai.llm;

import java.io.Serializable;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiDefaultEnums;

public enum AiProvider implements Serializable {
    // [impl->req~ai.llms.providers.openai~1]
    // [impl->req~ai.llms.custom.openai-compatible~1]
    OPEN_AI(Localization.lang("OpenAI (or API compatible)"), "https://api.openai.com/v1", "https://openai.com/policies/privacy-policy/"),
    // [impl->req~ai.llms.providers.mistral~1]
    MISTRAL_AI(Localization.lang("Mistral AI"), "https://api.mistral.ai/v1", "https://mistral.ai/terms/#privacy-policy"),
    // [impl->req~ai.llms.providers.gemini~1]
    GEMINI(Localization.lang("Gemini"), "https://generativelanguage.googleapis.com/v1beta/", "https://ai.google.dev/gemini-api/terms"),
    // [impl->req~ai.llms.providers.huggingface~1]
    HUGGING_FACE(Localization.lang("Hugging Face"), "https://router.huggingface.co/v1", "https://huggingface.co/privacy");

    private final String displayName;
    private final String apiUrl;
    private final String privacyPolicyUrl;

    AiProvider(String displayName, String apiUrl, String privacyPolicyUrl) {
        this.displayName = displayName;
        this.apiUrl = apiUrl;
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }

    public String toString() {
        return displayName;
    }

    public static AiProvider safeValueOf(String name) {
        try {
            return AiProvider.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.AI_PROVIDER;
        }
    }
}
