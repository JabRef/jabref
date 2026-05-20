package org.jabref.logic.ai.summarization.logic;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public class BibEntrySummarizator {
    // TODO: Simplify this class.
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BibEntrySummarizator.class);

    private final FilePreferences filePreferences;

    private final Summarizator summarizator;
    private final UniversalContentParser universalFileParser = new UniversalContentParser();

    public BibEntrySummarizator(
            @NonNull FilePreferences filePreferences,
            @NonNull Summarizator summarizator
    ) {
        this.filePreferences = filePreferences;
        this.summarizator = summarizator;
    }

    public AiSummary summarize(
            ChatModel chatModel,
            BibDatabaseContext bibDatabaseContext,
            BibEntry entry
    ) throws InterruptedException {
        String citationKey = entry.getCitationKey().orElse("<no citation key>");

        // Rationale for RuntimeException here:
        // It follows the same idiom as in langchain4j. See {@link JabRefChatLanguageModel.generate}, this method
        // is used internally in the summarization, and it also throws RuntimeExceptions.

        // Stream API would look better here, but we need to catch InterruptedException.
        List<String> linkedFilesSummary = new ArrayList<>();
        for (LinkedFile linkedFile : entry.getFiles()) {
            generateSummary(
                    chatModel,
                    bibDatabaseContext,
                    linkedFile,
                    citationKey
            )
                    .ifPresent(linkedFilesSummary::add);
        }

        if (linkedFilesSummary.isEmpty()) {
            throw new RuntimeException(Localization.lang("No summary can be generated for entry '%0'. Could not find attached linked files.", citationKey));
        }

        LOGGER.debug("All summaries for attached files of entry {} are generated. Generating final summary.", citationKey);

        String finalSummary;

        if (linkedFilesSummary.size() == 1) {
            finalSummary = linkedFilesSummary.getFirst();
        } else {
            finalSummary = summarizeSeveralDocuments(
                    chatModel,
                    linkedFilesSummary.stream()
            );
        }

        return new AiSummary(
                new AiMetadata(chatModel.getAiProvider(), chatModel.getName(), Instant.now()),
                summarizator.getKind(),
                finalSummary
        );
    }

    private Optional<String> generateSummary(
            ChatModel chatModel,
            BibDatabaseContext bibDatabaseContext,
            LinkedFile linkedFile,
            String citationKey
    ) throws InterruptedException {
        LOGGER.debug("Generating summary for file \"{}\" of entry {}", linkedFile.getLink(), citationKey);

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\" of entry {}", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        Optional<String> document = universalFileParser.parse(path.get());

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        if (document.isEmpty()) {
            LOGGER.warn("Could not extract text from a linked file \"{}\" of entry {}. It will be skipped when generating a summary.", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        String linkedFileSummary = summarizator.summarize(
                chatModel,
                document.get()
        );

        LOGGER.debug("BibEntrySummary for file \"{}\" of entry {} was generated successfully", linkedFile.getLink(), citationKey);
        return Optional.of(linkedFileSummary);
    }

    public String summarizeSeveralDocuments(
            ChatModel chatModel,
            Stream<String> documents
    ) throws InterruptedException {
        return summarizator.summarize(
                chatModel,
                documents.collect(Collectors.joining("\n\n"))
        );
    }
}
