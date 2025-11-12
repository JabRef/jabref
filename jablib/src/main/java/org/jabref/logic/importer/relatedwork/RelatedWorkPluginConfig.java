package org.jabref.logic.importer.relatedwork;

import java.util.Objects;

/**
 * Feature flags and plug-ins for optional summarization and citation resolution.
 * Defaults are no-ops with both features disabled to preserve current behavior.
 */
public final class RelatedWorkPluginConfig {

    private final boolean summarizationEnabled;
    private final boolean resolutionEnabled;

    private final RelatedWorkSummarizer summarizer;
    private final CitationResolver resolver;

    private RelatedWorkPluginConfig(boolean summarizationEnabled,
                                    boolean resolutionEnabled,
                                    RelatedWorkSummarizer summarizer,
                                    CitationResolver resolver) {
        this.summarizationEnabled = summarizationEnabled;
        this.resolutionEnabled = resolutionEnabled;
        this.summarizer = Objects.requireNonNull(summarizer);
        this.resolver = Objects.requireNonNull(resolver);
    }

    public boolean isSummarizationEnabled() {
        return summarizationEnabled;
    }

    public boolean isResolutionEnabled() {
        return resolutionEnabled;
    }

    public RelatedWorkSummarizer summarizer() {
        return summarizer;
    }

    public CitationResolver resolver() {
        return resolver;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder with safe defaults (both features off, no-op plugins).
     */
    public static final class Builder {
        private boolean summarizationEnabled = false;
        private boolean resolutionEnabled = false;
        private RelatedWorkSummarizer summarizer = new NoOpRelatedWorkSummarizer();
        private CitationResolver resolver = new NoOpCitationResolver();

        public Builder enableSummarization(boolean enabled) {
            this.summarizationEnabled = enabled;
            return this;
        }

        public Builder enableResolution(boolean enabled) {
            this.resolutionEnabled = enabled;
            return this;
        }

        public Builder withSummarizer(RelatedWorkSummarizer summarizer) {
            this.summarizer = Objects.requireNonNull(summarizer);
            return this;
        }

        public Builder withResolver(CitationResolver resolver) {
            this.resolver = Objects.requireNonNull(resolver);
            return this;
        }

        public RelatedWorkPluginConfig build() {
            return new RelatedWorkPluginConfig(summarizationEnabled, resolutionEnabled, summarizer, resolver);
        }
    }
}
