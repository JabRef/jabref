package org.jabref.logic.importer.relatedwork;

import java.util.Objects;

/**
 * Configuration holder for the Related Work plug-in.
 * <p>
 * For now it carries:
 * - a flag indicating whether AI summarization is enabled
 * - an optional {@link RelatedWorkSummarizer} implementation
 * <p>
 * If summarizationEnabled is false or the summarizer is null, the pipeline
 * should behave as if AI summarization is off.
 */
public final class RelatedWorkPluginConfig {

    private final boolean summarizationEnabled;
    private final RelatedWorkSummarizer summarizer;

    private RelatedWorkPluginConfig(Builder builder) {
        this.summarizationEnabled = builder.summarizationEnabled;
        this.summarizer = builder.summarizer;
    }

    public boolean isSummarizationEnabled() {
        return summarizationEnabled;
    }

    /**
     * @return the configured summarizer, or {@code null} if AI summarization is disabled.
     */
    public RelatedWorkSummarizer getSummarizer() {
        return summarizer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean summarizationEnabled;
        private RelatedWorkSummarizer summarizer;

        /**
         * Enable or disable AI summarization at the configuration level.
         */
        public Builder enableSummarization(boolean enabled) {
            this.summarizationEnabled = enabled;
            return this;
        }

        /**
         * Configure the summarizer implementation to use when summarization is enabled.
         */
        public Builder withSummarizer(RelatedWorkSummarizer summarizer) {
            this.summarizer = Objects.requireNonNull(summarizer);
            return this;
        }

        public RelatedWorkPluginConfig build() {
            return new RelatedWorkPluginConfig(this);
        }
    }
}
