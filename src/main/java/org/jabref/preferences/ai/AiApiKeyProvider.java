package org.jabref.preferences.ai;

public interface AiApiKeyProvider {
    String getApiKeyForAiProvider(AiProvider provider);

    void storeAiApiKeyInKeyring(AiProvider aiProvider, String newKey);
}
