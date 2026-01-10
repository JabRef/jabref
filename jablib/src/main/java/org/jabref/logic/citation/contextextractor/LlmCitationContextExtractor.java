package org.jabref.logic.citation.contextextractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.model.citation.CitationContext;
import org.jabref.model.citation.CitationContextList;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LlmCitationContextExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LlmCitationContextExtractor.class);

    private static final Pattern CITATION_BLOCK_PATTERN = Pattern.compile(
            "---CITATION---\\s*MARKER:\\s*(.+?)\\s*CONTEXT:\\s*(.+?)\\s*---END---",
            Pattern.DOTALL
    );

    private static final int MAX_CHUNK_SIZE = 4000;

    private final AiTemplatesService aiTemplatesService;
    private final ChatModel chatModel;

    public LlmCitationContextExtractor(AiTemplatesService aiTemplatesService, ChatModel chatModel) {
        this.aiTemplatesService = Objects.requireNonNull(aiTemplatesService, "AiTemplatesService cannot be null");
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel cannot be null");
    }

    public CitationContextList extractContexts(String text, String sourceCitationKey) {
        Objects.requireNonNull(text, "Text cannot be null");
        Objects.requireNonNull(sourceCitationKey, "Source citation key cannot be null");

        CitationContextList result = new CitationContextList(sourceCitationKey);

        if (text.isBlank()) {
            return result;
        }

        List<String> chunks = splitIntoChunks(text);
        LOGGER.debug("Split text into {} chunks for LLM processing", chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            LOGGER.debug("Processing chunk {}/{} ({} characters)", i + 1, chunks.size(), chunk.length());

            try {
                List<CitationContext> contexts = extractFromChunk(chunk, sourceCitationKey);
                result.addAll(contexts);
                LOGGER.debug("Extracted {} contexts from chunk {}", contexts.size(), i + 1);
            } catch (Exception e) {
                LOGGER.warn("Failed to extract contexts from chunk {}", i + 1, e);
            }
        }

        return result;
    }

    private List<CitationContext> extractFromChunk(String chunk, String sourceCitationKey) {
        String systemMessage = aiTemplatesService.makeCitationContextExtractionSystemMessage();
        String userMessage = aiTemplatesService.makeCitationContextExtractionUserMessage(chunk);

        String llmResponse = chatModel.chat(
                List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )
        ).aiMessage().text();

        LOGGER.trace("LLM response for citation extraction: {}", llmResponse);

        return parseLlmResponse(llmResponse, sourceCitationKey);
    }

    private List<CitationContext> parseLlmResponse(String response, String sourceCitationKey) {
        List<CitationContext> contexts = new ArrayList<>();

        if (response == null || response.isBlank()) {
            return contexts;
        }

        Matcher matcher = CITATION_BLOCK_PATTERN.matcher(response);

        while (matcher.find()) {
            String marker = matcher.group(1).trim();
            String contextText = matcher.group(2).trim();

            if (!marker.isBlank() && !contextText.isBlank()) {
                contexts.add(new CitationContext(marker, contextText, sourceCitationKey));
            }
        }

        if (contexts.isEmpty() && response.contains("MARKER:")) {
            LOGGER.debug("Standard parsing failed, attempting fallback parsing");
            contexts.addAll(fallbackParsing(response, sourceCitationKey));
        }

        return contexts;
    }

    private List<CitationContext> fallbackParsing(String response, String sourceCitationKey) {
        List<CitationContext> contexts = new ArrayList<>();

        String[] lines = response.split("\n");
        String currentMarker = null;
        StringBuilder currentContext = new StringBuilder();

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("MARKER:")) {
                if (currentMarker != null && !currentContext.isEmpty()) {
                    contexts.add(new CitationContext(currentMarker, currentContext.toString().trim(), sourceCitationKey));
                }
                currentMarker = line.substring("MARKER:".length()).trim();
                currentContext = new StringBuilder();
            } else if (line.startsWith("CONTEXT:")) {
                currentContext.append(line.substring("CONTEXT:".length()).trim());
            } else if (currentMarker != null && !line.startsWith("---") && !line.isBlank()) {
                if (!currentContext.isEmpty()) {
                    currentContext.append(" ");
                }
                currentContext.append(line);
            }
        }

        if (currentMarker != null && !currentContext.isEmpty()) {
            contexts.add(new CitationContext(currentMarker, currentContext.toString().trim(), sourceCitationKey));
        }

        return contexts;
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= MAX_CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }

        String[] paragraphs = text.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() + 2 > MAX_CHUNK_SIZE) {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }

                if (paragraph.length() > MAX_CHUNK_SIZE) {
                    chunks.addAll(splitLongParagraph(paragraph));
                } else {
                    currentChunk.append(paragraph);
                }
            } else {
                if (!currentChunk.isEmpty()) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() + 1 > MAX_CHUNK_SIZE) {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
            }
            if (!currentChunk.isEmpty()) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
