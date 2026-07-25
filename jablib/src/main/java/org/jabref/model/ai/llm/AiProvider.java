package org.jabref.model.ai.llm;

import java.io.Serializable;

import org.jabref.model.ai.AiDefaultEnums;

public enum AiProvider implements Serializable {
    // [impl->req~ai.llms.providers.openai~1]
    // [impl->req~ai.llms.custom.openai-compatible~1]
    OPEN_AI("https://api.openai.com/v1", "https://openai.com/policies/privacy-policy/"),
    // [impl->req~ai.llms.providers.mistral~1]
    MISTRAL_AI("https://api.mistral.ai/v1", "https://mistral.ai/terms/#privacy-policy"),
    // [impl->req~ai.llms.providers.gemini~1]
    GEMINI("https://generativelanguage.googleapis.com/v1beta/", "https://ai.google.dev/gemini-api/terms"),
    // [impl->req~ai.llms.providers.huggingface~1]
    HUGGING_FACE("https://router.huggingface.co/v1", "https://huggingface.co/privacy");

    private final String apiUrl;
    private final String privacyPolicyUrl;

    AiProvider(String apiUrl, String privacyPolicyUrl) {
        this.apiUrl = apiUrl;
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }

    public static AiProvider safeValueOf(String name) {
        try {
            return AiProvider.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AiDefaultEnums.AI_PROVIDER;
        }
    }
}
