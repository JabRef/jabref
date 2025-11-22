package org.jabref.logic.importer.relatedwork;

import java.util.Objects;

/**
 * Small value object for Related Work AI configuration.
 * <p>
 * This class deliberately does NOT know about JabRefPreferences to keep jablib
 * independent of the GUI/application layer. Construction from preferences
 * should be done in a higher-level module.
 */
public final class RelatedWorkAiPreferences {

    private final boolean enabled;
    private final String modelName;
    private final String apiKeyEnvVar;

    public RelatedWorkAiPreferences(boolean enabled, String modelName, String apiKeyEnvVar) {
        this.enabled = enabled;
        this.modelName = Objects.requireNonNullElse(modelName, "").trim();
        this.apiKeyEnvVar = Objects.requireNonNullElse(apiKeyEnvVar, "").trim();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getModelName() {
        return modelName;
    }

    public String getApiKeyEnvVar() {
        return apiKeyEnvVar;
    }
}
