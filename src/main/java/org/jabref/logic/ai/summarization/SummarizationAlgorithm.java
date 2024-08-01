package org.jabref.logic.ai.summarization;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

public class SummarizationAlgorithm {
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

    public static String summarize(ChatLanguageModel chatLanguageModel, int contextWindowSize, String content) {
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(contextWindowSize - MAX_OVERLAP_SIZE_IN_CHARS * 2 - estimateTokenCount(CHUNK_PROMPT_TEMPLATE), MAX_OVERLAP_SIZE_IN_CHARS);

        List<String> chunkSummaries;

        do {
            chunkSummaries = documentSplitter.split(new Document(content)).stream().map(TextSegment::text).map(doc -> {
                // Be careful, langchain really requires that it should be a map of strings to objects.
                // Source: dev.langchain4j.model.input.PromptTemplate.apply(java.util.Map<java.lang.String,java.lang.Object>)
                Prompt prompt = CHUNK_PROMPT_TEMPLATE.apply(Collections.singletonMap("document", content));

                return chatLanguageModel.generate(prompt.toString());
            }).toList();
        } while (estimateTokenCount(chunkSummaries) > contextWindowSize - estimateTokenCount(COMBINE_PROMPT_TEMPLATE));

        if (chunkSummaries.size() == 1) {
            return chunkSummaries.getFirst();
        }

        Prompt prompt = COMBINE_PROMPT_TEMPLATE.apply(Collections.singletonMap("summaries", String.join("\n\n", chunkSummaries)));

        return chatLanguageModel.generate(prompt.toString());
    }

    public static String summarize(ChatLanguageModel chatLanguageModel, int contextWindowSize, Stream<String> chunks) {
        return summarize(chatLanguageModel, contextWindowSize, chunks.collect(Collectors.joining("\n\n")));
    }

    private static int estimateTokenCount(List<String> chunkSummaries) {
        return chunkSummaries.stream().mapToInt(SummarizationAlgorithm::estimateTokenCount).sum();
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
}
