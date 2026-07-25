package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.summarization.SummarizatorKind;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->req~ai.summarization.general.unlimited-size~1]
// [impl->feat~ai.summarization.algorithms.chunked~1]
public class ChunkedSummarizator implements Summarizator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedSummarizator.class);

    // TODO: Make a parameter?
    private static final int MAX_OVERLAP_SIZE_IN_CHARS = 100;

    private final String summarizationChunkSystemMessageTemplate;
    private final String summarizationCombineSystemMessageTemplate;

    public ChunkedSummarizator(
            String summarizationChunkSystemMessageTemplate,
            String summarizationCombineSystemMessageTemplate
    ) {
        this.summarizationChunkSystemMessageTemplate = summarizationChunkSystemMessageTemplate;
        this.summarizationCombineSystemMessageTemplate = summarizationCombineSystemMessageTemplate;
    }

    @Override
    public String summarize(ChatModel chatModel, String text) throws InterruptedException {
        LOGGER.debug("Summarizing text ({} chars)", text.length());

        List<String> summaries = splitTextIntoChunks(chatModel, text);
        LOGGER.debug("Text was split into {} chunks", summaries.size());

        int passes = 0;

        // @formatter:off
        do {
            // @formatter:on
            passes++;
            LOGGER.debug("Summarizing pass {} ({} chunks)", passes, summaries.size());

            summaries = summarizeChunks(chatModel, summaries, passes);
        } while (needsAnotherPass(chatModel, summaries));

        return combineFinalSummaries(chatModel, summaries);
    }

    private List<String> splitTextIntoChunks(ChatModel chatModel, String text) {
        int chunkSystemMessageTokens = chatModel.getTokenizer().estimate(
                ChatMessage.Role.SYSTEM,
                summarizationChunkSystemMessageTemplate
        );

        int maxChunkSize = chatModel.getContextWindowSize() - MAX_OVERLAP_SIZE_IN_CHARS * 2 - chunkSystemMessageTokens;

        DocumentSplitter splitter = DocumentSplitters.recursive(maxChunkSize, MAX_OVERLAP_SIZE_IN_CHARS);

        return splitter.split(new DefaultDocument(text)).stream()
                       .map(TextSegment::text)
                       .toList();
    }

    private boolean needsAnotherPass(ChatModel chatModel, List<String> summaries) {
        int combineSystemMessageTokens = chatModel.getTokenizer().estimate(
                ChatMessage.Role.SYSTEM,
                summarizationCombineSystemMessageTemplate
        );

        int summariesTokens = chatModel.getTokenizer().estimate(
                summaries.stream().map(ChatMessage::userMessage).toList()
        );

        return summariesTokens > chatModel.getContextWindowSize() - combineSystemMessageTokens;
    }

    private List<String> summarizeChunks(ChatModel chatModel, List<String> chunks, int passNumber) throws InterruptedException {
        List<String> summarizedChunks = new ArrayList<>();

        for (String chunk : chunks) {
            checkInterrupted();

            String summary = summarizeChunk(chatModel, chunk);
            summarizedChunks.add(summary);

            LOGGER.debug("Chunk summary (pass {}) generated successfully", passNumber);
        }

        return summarizedChunks;
    }

    private String summarizeChunk(ChatModel chatModel, String chunk) {
        String systemMessage = AiTemplateRenderer.renderSummarizationChunkSystemMessage(summarizationChunkSystemMessageTemplate);

        LOGGER.debug("Sending request to AI provider to summarize a chunk");
        return chatModel.chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(chunk)
        )).aiMessage().text();
    }

    private String combineFinalSummaries(ChatModel chatModel, List<String> summaries) throws InterruptedException {
        if (summaries.size() == 1) {
            LOGGER.debug("BibEntrySummary of the text was generated successfully");
            return summaries.getFirst();
        }

        checkInterrupted();

        String systemMessage = AiTemplateRenderer.renderSummarizationCombineSystemMessage(summarizationCombineSystemMessageTemplate);

        LOGGER.debug("Sending request to AI provider to combine summary chunks");
        String result = chatModel.chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(String.join("\n\n", summaries))
        )).aiMessage().text();

        LOGGER.debug("BibEntrySummary of the text was generated successfully");
        return result;
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    @Override
    public SummarizatorKind getKind() {
        return SummarizatorKind.CHUNKED;
    }
}
