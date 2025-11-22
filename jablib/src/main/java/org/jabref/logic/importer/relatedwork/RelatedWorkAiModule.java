package org.jabref.logic.importer.relatedwork;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory / wiring helper that builds a {@link RelatedWorkPluginConfig}
 * from {@link RelatedWorkAiPreferences} and an optional
 * {@link RelatedWorkSummarizer} implementation.
 * <p>
 * This class deliberately does NOT depend on LangChain4j or any concrete
 * LLM client. Higher-level modules are responsible for
 * constructing a {@link RelatedWorkSummarizer} (such as
 * {@code LangChainRelatedWorkSummarizer}) and passing it in.
 */
public final class RelatedWorkAiModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedWorkAiModule.class);

    private RelatedWorkAiModule() {
        // utility
    }

    /**
     * Build a plugin config from preferences and an optional summarizer.
     * <p>
     * If AI is disabled in {@code aiPrefs} or {@code summarizer} is {@code null},
     * this returns a config with summarization effectively turned off.
     *
     * @param aiPrefs user / application preferences for related-work AI
     * @param summarizer an optional summarizer implementation (may be null)
     * @return a {@link RelatedWorkPluginConfig} representing the effective setup
     */
    public static RelatedWorkPluginConfig fromPreferences(
            RelatedWorkAiPreferences aiPrefs,
            RelatedWorkSummarizer summarizer
    ) {
        Objects.requireNonNull(aiPrefs);

        RelatedWorkPluginConfig.Builder builder = RelatedWorkPluginConfig.builder();

        if (!aiPrefs.isEnabled()) {
            LOGGER.info("Related Work AI is disabled via preferences");
            return builder.build();
        }

        if (summarizer == null) {
            LOGGER.info("Related Work AI is enabled in preferences, but no summarizer implementation was provided; " + "falling back to no-op configuration.");
            return builder.build();
        }

        LOGGER.info("Related Work AI enabled (model='{}', apiKeyEnvVar='{}')",
                aiPrefs.getModelName(),
                aiPrefs.getApiKeyEnvVar());

        return builder
                .enableSummarization(true)
                .withSummarizer(summarizer)
                .build();
    }
}
