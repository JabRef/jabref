package org.jabref.model.ai;

import java.io.Serializable;

public enum AiProvider implements Serializable {
    OPEN_AI("OpenAI", "https://api.openai.com/v1", "https://openai.com/policies/privacy-policy/"),
    MISTRAL_AI("Mistral AI", "https://api.mistral.ai/v1", "https://mistral.ai/terms/#privacy-policy"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta/", "https://ai.google.dev/gemini-api/terms"),
    HUGGING_FACE("Hugging Face", "https://huggingface.co/api", "https://huggingface.co/privacy"),
    GPT4ALL("GPT4All", "http://localhost:4891/v1", "https://www.nomic.ai/gpt4all/legal/privacy-policy");

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

