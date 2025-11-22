package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RelatedWorkSummarizer implementation that delegates summarization to an LLM client.
 *
 * <p>To keep this class easy to test and free from hard dependencies, the actual
 * model is abstracted behind the {@link Client} functional interface. In production,
 * callers can wrap a LangChain4j ChatLanguageModel:</p>
 *
 * <pre>{@code
 * ChatLanguageModel model = OpenAiChatModel.builder()
 *     .apiKey(apiKey)
 *     .modelName(modelName)
 *     .build();
 *
 * LangChainRelatedWorkSummarizer summarizer =
 *     new LangChainRelatedWorkSummarizer(model::generate);
 * }</pre>
 */
public final class LangChainRelatedWorkSummarizer implements RelatedWorkSummarizer {

    /**
     * Minimal abstraction over an LLM-like client.
     * Implementations are expected to synchronously return a string for a prompt.
     */
    @FunctionalInterface
    public interface Client {
        String generate(String prompt);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LangChainRelatedWorkSummarizer.class);

    private static final int DEFAULT_MAX_LEN = 350;

    private final Client client;

    public LangChainRelatedWorkSummarizer(Client client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public Optional<String> summarize(List<String> snippets, BibEntry entry, int maxLen) {
        if (snippets == null || snippets.isEmpty()) {
            return Optional.empty();
        }

        int effectiveMaxLen = maxLen > 0 ? maxLen : DEFAULT_MAX_LEN;

        String prompt = buildPrompt(snippets, entry, effectiveMaxLen);

        try {
            String raw = client.generate(prompt);
            if (raw == null) {
                return Optional.empty();
            }
            String cleaned = normalize(raw);
            if (cleaned.isBlank()) {
                return Optional.empty();
            }
            if (cleaned.length() > effectiveMaxLen) {
                cleaned = cleaned.substring(0, effectiveMaxLen).trim();
            }
            return Optional.of(cleaned);
        } catch (Exception e) {
            // Fail-safe: never break the harvester due to AI issues.
            LOGGER.warn("LangChain summarizer failed for entry {}",
                    entry.getCitationKey().orElse("<no-key>"), e);
            return Optional.empty();
        }
    }

    private String buildPrompt(List<String> snippets, BibEntry entry, int maxLen) {
        StringJoiner joiner = new StringJoiner("\n- ", "- ", "");

        for (String s : snippets) {
            if (s != null && !s.isBlank()) {
                joiner.add(s.trim());
            }
        }

        String citationKey = entry.getCitationKey().orElse("");
        String title = entry.getField(StandardField.TITLE).orElse("this paper");

        return "You are helping to summarize how one academic paper describes another in its "
                + "\"Related Work\" section.\n\n"
                + "Cited paper bibtex key (if known): " + citationKey + "\n"
                + "Cited paper title (if known): " + title + "\n\n"
                + "Task:\n"
                + "Using only the fragments below, write a concise, neutral 1–2 sentence description "
                + "of the cited paper's contribution, as characterized by the citing paper.\n"
                + "Do not invent new facts. Do not mention citation keys or authors by name.\n"
                + "Maximum length: approximately " + maxLen + " characters.\n\n"
                + "Fragments:\n"
                + joiner.toString();
    }

    private String normalize(String text) {
        String trimmed = text.trim();

        // Strip a leading "Summary:" prefix if the model adds one
        if (trimmed.toLowerCase().startsWith("summary:")) {
            trimmed = trimmed.substring("summary:".length()).trim();
        }
        // Strip surrounding quotes
        if (trimmed.length() > 1 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        return trimmed;
    }
}
