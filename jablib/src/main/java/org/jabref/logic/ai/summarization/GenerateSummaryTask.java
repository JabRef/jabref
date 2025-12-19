package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.ingestion.FileToDocument;
import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates a new summary for an entry.
 * It will check if summary was already generated.
 * And it also will store the summary.
 * <p>
 * This task is created in the {@link SummariesService}, and stored then in a {@link SummariesStorage}.
 */
public class GenerateSummaryTask extends BackgroundTask<Summary> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryTask.class);

    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 100;
    private static final int CHAR_TOKEN_FACTOR = 4; // Means, every token is roughly 4 characters.

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final String citationKey;
    private final ChatModel chatLanguageModel;
    private final SummariesStorage summariesStorage;
    private final AiTemplatesService aiTemplatesService;
    private final ReadOnlyBooleanProperty shutdownSignal;
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public GenerateSummaryTask(BibEntry entry,
                               BibDatabaseContext bibDatabaseContext,
                               SummariesStorage summariesStorage,
                               ChatModel chatLanguageModel,
                               AiTemplatesService aiTemplatesService,
                               ReadOnlyBooleanProperty shutdownSignal,
                               AiPreferences aiPreferences,
                               FilePreferences filePreferences
    ) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.citationKey = entry.getCitationKey().orElse("<no citation key>");
        this.chatLanguageModel = chatLanguageModel;
        this.summariesStorage = summariesStorage;
        this.aiTemplatesService = aiTemplatesService;
        this.shutdownSignal = shutdownSignal;
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;

        configure();
    }

    private void configure() {
        showToUser(true);
        titleProperty().set(Localization.lang("Summarizing   %0...", citationKey));

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    public Summary call() {
        LOGGER.debug("Starting summarization task for entry {}", citationKey);

        Optional<Summary> savedSummary = Optional.empty();

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.info("No citation key is present. Summary will not be stored in the next sessions");
        } else {
            savedSummary = summariesStorage.get(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());
        }

        Summary summary;

        if (savedSummary.isPresent()) {
            summary = savedSummary.get();
        } else {
            try {
                String result = summarizeAll();

                summary = new Summary(
                        LocalDateTime.now(),
                        aiPreferences.getAiProvider(),
                        aiPreferences.getSelectedChatModel(),
                        result
                );
            } catch (InterruptedException e) {
                LOGGER.debug("There was a summarization task for {}. It will be canceled, because user quits JabRef.", citationKey);
                return null;
            }
        }

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
        } else if (CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            LOGGER.info("No valid citation key is present. Summary will not be stored in the next sessions");
        } else {
            summariesStorage.set(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get(), summary);
        }

        LOGGER.debug("Finished summarization task for entry {}", citationKey);
        progressCounter.stop();

        return summary;
    }

    private String summarizeAll() throws InterruptedException {
        // Rationale for RuntimeException here:
        // It follows the same idiom as in langchain4j. See {@link JabRefChatLanguageModel.generate}, this method
        // is used internally in the summarization, and it also throws RuntimeExceptions.

        // Stream API would look better here, but we need to catch InterruptedException.
        List<String> linkedFilesSummary = new ArrayList<>();
        for (LinkedFile linkedFile : entry.getFiles()) {
            generateSummary(linkedFile).ifPresent(linkedFilesSummary::add);
        }

        if (linkedFilesSummary.isEmpty()) {
            doneOneWork(); // Skipped generation of final summary.
            throw new RuntimeException(Localization.lang("No summary can be generated for entry '%0'. Could not find attached linked files.", citationKey));
        }

        LOGGER.debug("All summaries for attached files of entry {} are generated. Generating final summary.", citationKey);

        String finalSummary;

        addMoreWork(1); // For generating final summary.

        if (linkedFilesSummary.size() == 1) {
            finalSummary = linkedFilesSummary.getFirst();
        } else {
            finalSummary = summarizeSeveralDocuments(linkedFilesSummary.stream());
        }

        doneOneWork();

        return finalSummary;
    }

    private Optional<String> generateSummary(LinkedFile linkedFile) throws InterruptedException {
        LOGGER.debug("Generating summary for file \"{}\" of entry {}", linkedFile.getLink(), citationKey);

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\" of entry {}", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        Optional<Document> document = new FileToDocument(shutdownSignal).fromFile(path.get());

        if (document.isEmpty()) {
            LOGGER.warn("Could not extract text from a linked file \"{}\" of entry {}. It will be skipped when generating a summary.", linkedFile.getLink(), citationKey);
            LOGGER.debug("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        String linkedFileSummary = summarizeOneDocument(path.get().toString(), document.get().text());

        LOGGER.debug("Summary for file \"{}\" of entry {} was generated successfully", linkedFile.getLink(), citationKey);
        return Optional.of(linkedFileSummary);
    }

    public String summarizeOneDocument(String filePath, String document) throws InterruptedException {
        addMoreWork(1); // For the combination of summary chunks.

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(
                aiPreferences.getContextWindowSize() - MAX_OVERLAP_SIZE_IN_CHARS * 2 - estimateTokenCount(aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE)),
                MAX_OVERLAP_SIZE_IN_CHARS
        );

        List<String> chunkSummaries = documentSplitter.split(new DefaultDocument(document)).stream().map(TextSegment::text).toList();

        LOGGER.debug("The file \"{}\" of entry {} was split into {} chunk(s)", filePath, citationKey, chunkSummaries.size());

        int passes = 0;

        // @formatter:off
        do {
            // @formatter:on
            passes++;
            LOGGER.debug("Summarizing chunk(s) for file \"{}\" of entry {} ({} pass)", filePath, citationKey, passes);

            addMoreWork(chunkSummaries.size());

            List<String> list = new ArrayList<>();

            for (String chunkSummary : chunkSummaries) {
                if (shutdownSignal.get()) {
                    throw new InterruptedException();
                }

                String systemMessage = aiTemplatesService.makeSummarizationChunkSystemMessage();
                String userMessage = aiTemplatesService.makeSummarizationChunkUserMessage(chunkSummary);

                LOGGER.debug("Sending request to AI provider to summarize a chunk from file \"{}\" of entry {}", filePath, citationKey);
                String chunk = chatLanguageModel.chat(List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )).aiMessage().text();
                LOGGER.debug("Chunk summary for file \"{}\" of entry {} was generated successfully", filePath, citationKey);

                list.add(chunk);
                doneOneWork();
            }

            chunkSummaries = list;
        } while (estimateTokenCount(chunkSummaries) > aiPreferences.getContextWindowSize() - estimateTokenCount(aiPreferences.getTemplate(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE)));

        if (chunkSummaries.size() == 1) {
            doneOneWork(); // No need to call LLM for combination of summary chunks.
            LOGGER.debug("Summary of the file \"{}\" of entry {} was generated successfully", filePath, citationKey);
            return chunkSummaries.getFirst();
        }

        String systemMessage = aiTemplatesService.makeSummarizationCombineSystemMessage();
        String userMessage = aiTemplatesService.makeSummarizationCombineUserMessage(chunkSummaries);

        if (shutdownSignal.get()) {
            throw new InterruptedException();
        }

        LOGGER.debug("Sending request to AI provider to combine summary chunk(s) for file \"{}\" of entry {}", filePath, citationKey);
        String result = chatLanguageModel.chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(userMessage)
        )).aiMessage().text();
        LOGGER.debug("Summary of the file \"{}\" of entry {} was generated successfully", filePath, citationKey);

        doneOneWork();
        return result;
    }

    public String summarizeSeveralDocuments(Stream<String> documents) throws InterruptedException {
        return summarizeOneDocument(citationKey, documents.collect(Collectors.joining("\n\n")));
    }

    private static int estimateTokenCount(List<String> chunkSummaries) {
        return chunkSummaries.stream().mapToInt(GenerateSummaryTask::estimateTokenCount).sum();
    }

    private static int estimateTokenCount(String string) {
        return estimateTokenCount(string.length());
    }

    private static int estimateTokenCount(int numOfChars) {
        return numOfChars / CHAR_TOKEN_FACTOR;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }

    private void addMoreWork(int moreWork) {
        progressCounter.increaseWorkMax(moreWork);
        updateProgress();
    }

    private void doneOneWork() {
        progressCounter.increaseWorkDone(1);
        updateProgress();
    }
}
