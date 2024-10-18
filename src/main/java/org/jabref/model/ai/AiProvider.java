package org.jabref.model.ai;

import java.io.Serializable;

public enum AiProvider implements Serializable {
    OPEN_AI("OpenAI", "https://openai.com/policies/privacy-policy/", "https://openai.com/policies/privacy-policy/"),
    MISTRAL_AI("Mistral AI", "https://mistral.ai/terms/#privacy-policy", "https://mistral.ai/terms/#privacy-policy"),
    GEMINI("Gemini", "https://huggingface.co/privacy", "https://ai.google.dev/gemini-api/terms"),
    HUGGING_FACE("Hugging Face", "https://huggingface.co/api", "https://huggingface.co/privacy");

    private final String label;
    private final String apiUrl;
    private final String privacyPolicyUrl;

    AiProvider(String label, String apiUrl, String privacyPolicyUrl) {
        this.label = label;
        this.apiUrl = apiUrl;
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public String getLabel() {
        return label;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }

    public String toString() {
        return label;
    }
}

