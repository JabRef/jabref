package org.jabref.logic.ai.summarization;

import java.util.List;
import java.util.Map;
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
    private static final String SYSTEM_MESSAGE = """
            You are an AI language model tasked with refining and summarizing \
            large texts. The process involves breaking down the text into smaller chunks, creating initial bullet \
            points for each chunk, and then refining these bullet points to ensure accuracy and clarity. Your goal \
            is to capture the most important information from each chunk and represent it concisely in the form of \
            bullet points.""";

    // Be careful when constructing prompt.
    // 1. It should contain variables `bullets` and `chunk`.
    // 2. Variables should be wrapped in `{{` and `}}` and only with them. No whitespace inside.
    private static final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
            """
                    **Task:** Refine the following list of bullet points based on the given chunk of text.

                    **Existing Bullet Points:**
                    {{bullets}}

                    **Text Chunk:**
                    {{chunk}}

                    **Refined Bullet Points:**"""
    );

    private static final DocumentSplitter DOCUMENT_SPLITTER = DocumentSplitters.recursive(3000, 100);

    // Note: content will be split in chunks.
    public static String summarize(ChatLanguageModel chatLanguageModel, String content) {
        Stream<String> chunks = DOCUMENT_SPLITTER
                .split(new Document(content))
                .stream()
                .map(TextSegment::text);

        return summarize(chatLanguageModel, chunks);
    }

    // Note: chunks won't be split further.
    public static String summarize(ChatLanguageModel chatLanguageModel, Stream<String> chunks) {
        return chunks
                .reduce("", (bullets, chunk) -> {
                    List<ChatMessage> chatMessages = generateChatMessages(bullets, chunk);
                    Response<AiMessage> response = chatLanguageModel.generate(chatMessages);
                    return response.content().text();
                });
    }

    private static List<ChatMessage> generateChatMessages(String bullets, String chunk) {
        // Be careful, langchain really requires that it should be a map of strings to objects.
        // Source: dev.langchain4j.model.input.PromptTemplate.apply(java.util.Map<java.lang.String,java.lang.Object>)
        Map<String, Object> variables = Map.of(
                "bullets", bullets,
                "chunk", chunk
        );

        Prompt prompt = PROMPT_TEMPLATE.apply(variables);

        return List.of(
                new SystemMessage(chunk),
                new UserMessage(prompt.text())
        );
    }
}
