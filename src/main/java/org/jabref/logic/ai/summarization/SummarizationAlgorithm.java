package org.jabref.logic.ai.summarization;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;

public class SummarizationAlgorithm {
    // Be careful when constructing prompt.
    // 1. It should contain variables `bullets` and `chunk`.
    // 2. Variables should be wrapped in `{{` and `}}` and only with them. No whitespace inside.
    private static final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
            """
                Please provide an overview of the following academic paper.
                The summary should include the main objectives, methodologies used, key findings, and conclusions.
                Mention any significant experiments, data, or discussions presented in the paper.
                Write output in Markdown.

                DOCUMENT:
                {{document}}

                OVERVIEW:"""
    );

    public static String summarize(ChatLanguageModel chatLanguageModel, String content) {
        // Be careful, langchain really requires that it should be a map of strings to objects.
        // Source: dev.langchain4j.model.input.PromptTemplate.apply(java.util.Map<java.lang.String,java.lang.Object>)
        Prompt prompt = PROMPT_TEMPLATE.apply(Collections.singletonMap("document", content));

        return chatLanguageModel.generate(prompt.toString());
    }

    public static String summarize(ChatLanguageModel chatLanguageModel, Stream<String> chunks) {
        return summarize(chatLanguageModel, chunks.collect(Collectors.joining("\n\n")));
    }

}
