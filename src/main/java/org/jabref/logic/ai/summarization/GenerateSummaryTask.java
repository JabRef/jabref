package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.embeddings.FileToDocument;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSummaryTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryTask.class);

    // Be careful when constructing prompt.
    // 1. It should contain variables `bullets` and `chunk`.
    // 2. Variables should be wrapped in `{{` and `}}` and only with them. No whitespace inside.
    private static final PromptTemplate CHUNK_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                Please provide an overview of the following text. It's a part of a scientific paper.
                The summary should include the main objectives, methodologies used, key findings, and conclusions.
                Mention any significant experiments, data, or discussions presented in the paper.

                DOCUMENT:
                {{document}}

                OVERVIEW:"""
    );

    private static final PromptTemplate COMBINE_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                You have written an overview of a scientific paper. You have been collecting notes from various parts
                of the paper. Now your task is to combine all of the notes in one structured message.

                SUMMARIES:
                {{summaries}}

                FINAL OVERVIEW:"""
    );

    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 100;
    private static final int CHAR_TOKEN_FACTOR = 4; // Means, every token is roughly 4 characters.

    private final BibDatabaseContext bibDatabaseContext;
    private final String citationKey;
    private final List<LinkedFile> linkedFiles;
    private final AiService aiService;
    private final FilePreferences filePreferences;

    private final ProgressCounter progressCounter = new ProgressCounter();

    public GenerateSummaryTask(BibDatabaseContext bibDatabaseContext,
                               String citationKey,
                               List<LinkedFile> linkedFiles,
                               AiService aiService,
                               FilePreferences filePreferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.citationKey = citationKey;
        this.linkedFiles = linkedFiles;
        this.aiService = aiService;
        this.filePreferences = filePreferences;

        titleProperty().set(Localization.lang("Waiting summary for %0...", citationKey));
        showToUser(true);

        progressCounter.listenToAllProperties(this::updateProgress);
    }

    @Override
    protected Void call() throws Exception {
        LOGGER.info("Starting summarization task for entry {}", citationKey);

        try {
            summarizeAll();
        } catch (InterruptedException e) {
            LOGGER.info("There was a summarization task for {}. It will be canceled, because user quits JabRef.", citationKey);
        }

        showToUser(false);

        LOGGER.info("Finished summarization task for entry {}", citationKey);

        return null;
    }

    private void summarizeAll() throws InterruptedException {
        // Rationale for RuntimeException here:
        // It follows the same idiom as in langchain4j. See {@link JabRefChatLanguageModel.generate}, this method
        // is used internally in the summarization, and it also throws RuntimeExceptions.

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            throw new RuntimeException(Localization.lang("No summary can be generated for entry '%0' as the database does not have path", citationKey));
        }

        addMoreWork(1); // For generating final summary.

        // Stream API would look better here, but we need to catch InterruptedException.
        List<String> linkedFilesSummary = new ArrayList<>();
        for (LinkedFile linkedFile : linkedFiles) {
            Optional<String> s = generateSummary(linkedFile);
            if (s.isPresent()) {
                String string = s.get();
                linkedFilesSummary.add(string);
            }
        }

        if (linkedFilesSummary.isEmpty()) {
            doneOneWork(); // Skipped generation of final summary.
            throw new RuntimeException(Localization.lang("No summary can be generated for entry '%0'. Could not find attached linked files.", citationKey));
        }

        LOGGER.info("All summaries for attached files of entry {} are generated. Generating final summary.", citationKey);

        String finalSummary;

        if (linkedFilesSummary.size() == 1) {
            finalSummary = linkedFilesSummary.getFirst();
        } else {
            finalSummary = summarizeSeveralDocuments(linkedFilesSummary.stream());
        }

        doneOneWork();

        SummariesStorage.SummarizationRecord summaryRecord = new SummariesStorage.SummarizationRecord(
                LocalDateTime.now(),
                aiService.getPreferences().getAiProvider(),
                aiService.getPreferences().getSelectedChatModel(),
                finalSummary
        );

        aiService.getSummariesStorage().set(bibDatabaseContext.getDatabasePath().get(), citationKey, summaryRecord);
    }

    private Optional<String> generateSummary(LinkedFile linkedFile) throws InterruptedException {
        LOGGER.info("Generating summary for file \"{}\" of entry {}", linkedFile.getLink(), citationKey);

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\" of entry {}", linkedFile.getLink(), citationKey);
            LOGGER.info("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        Optional<Document> document = FileToDocument.fromFile(path.get());

        if (document.isEmpty()) {
            LOGGER.warn("Could not extract text from a linked file \"{}\" of entry {}. It will be skipped when generating a summary.", linkedFile.getLink(), citationKey);
            LOGGER.info("Unable to generate summary for file \"{}\" of entry {}, because it was not found", linkedFile.getLink(), citationKey);
            return Optional.empty();
        }

        String linkedFileSummary = summarizeOneDocument(path.get().toString(), document.get().text());

        LOGGER.info("Summary for file \"{}\" of entry {} was generated successfully", linkedFile.getLink(), citationKey);
        return Optional.of(linkedFileSummary);
    }

    public String summarizeOneDocument(String filePath, String document) throws InterruptedException {
        addMoreWork(1); // For the combination of summary chunks.

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(aiService.getPreferences().getContextWindowSize() - MAX_OVERLAP_SIZE_IN_CHARS * 2 - estimateTokenCount(CHUNK_PROMPT_TEMPLATE), MAX_OVERLAP_SIZE_IN_CHARS);

        List<String> chunkSummaries = documentSplitter.split(new Document(document)).stream().map(TextSegment::text).toList();

        LOGGER.info("The file \"{}\" of entry {} was split into {} chunk(s)", filePath, citationKey, chunkSummaries.size());

        int passes = 0;

        do {
            passes++;
            LOGGER.info("Summarizing chunk(s) for file \"{}\" of entry {} ({} pass)", filePath, citationKey, passes);

            addMoreWork(chunkSummaries.size());

            List<String> list = new ArrayList<>();

            for (String chunkSummary : chunkSummaries) {
                if (aiService.getShutdownSignal().get()) {
                    throw new InterruptedException();
                }

                Prompt prompt = CHUNK_PROMPT_TEMPLATE.apply(Collections.singletonMap("document", chunkSummary));

                LOGGER.info("Sending request to AI provider to summarize a chunk from file \"{}\" of entry {}", filePath, citationKey);
                String chunk = aiService.getChatLanguageModel().generate(prompt.toString());
                LOGGER.info("Chunk summary for file \"{}\" of entry {} was generated successfully", filePath, citationKey);

                list.add(chunk);
                doneOneWork();
            }

            chunkSummaries = list;
        } while (estimateTokenCount(chunkSummaries) > aiService.getPreferences().getContextWindowSize() - estimateTokenCount(COMBINE_PROMPT_TEMPLATE));

        if (chunkSummaries.size() == 1) {
            doneOneWork(); // No need to call LLM for combination of summary chunks.
            LOGGER.info("Summary of the file \"{}\" of entry {} was generated successfully", filePath, citationKey);
            return chunkSummaries.getFirst();
        }

        Prompt prompt = COMBINE_PROMPT_TEMPLATE.apply(Collections.singletonMap("summaries", String.join("\n\n", chunkSummaries)));

        if (aiService.getShutdownSignal().get()) {
            throw new InterruptedException();
        }

        LOGGER.info("Sending request to AI provider to combine summary chunk(s) for file \"{}\" of entry {}", filePath, citationKey);
        String result = aiService.getChatLanguageModel().generate(prompt.toString());
        LOGGER.info("Summary of the file \"{}\" of entry {} was generated successfully", filePath, citationKey);

        doneOneWork();
        return result;
    }

    public String summarizeSeveralDocuments(Stream<String> documents) throws InterruptedException {
        return summarizeOneDocument(citationKey, documents.collect(Collectors.joining("\n\n")));
    }

    private static int estimateTokenCount(List<String> chunkSummaries) {
        return chunkSummaries.stream().mapToInt(GenerateSummaryTask::estimateTokenCount).sum();
    }

    private static int estimateTokenCount(PromptTemplate promptTemplate) {
        return estimateTokenCount(promptTemplate.template());
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
