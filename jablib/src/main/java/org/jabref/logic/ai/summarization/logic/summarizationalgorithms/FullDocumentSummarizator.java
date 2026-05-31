package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import java.util.List;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.model.ai.summarization.SummarizatorKind;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->feat~ai.summarization.algorithms.full~1]
public class FullDocumentSummarizator implements Summarizator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullDocumentSummarizator.class);

    private final String systemMessageTemplate;

    public FullDocumentSummarizator(String systemMessageTemplate) {
        this.systemMessageTemplate = systemMessageTemplate;
    }

    @Override
    public String summarize(ChatModel chatModel, String text) throws InterruptedException {
        LOGGER.debug("Summarizing whole document ({} chars)", text.length());

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        String systemMessage = AiTemplateRenderer.renderSummarizationFullDocumentSystemMessage(systemMessageTemplate);

        LOGGER.debug("Sending request to AI provider to summarize the full document");
        String result = chatModel.chat(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(text)
        )).aiMessage().text();

        LOGGER.debug("Full-document summary was generated successfully");
        return result;
    }

    @Override
    public SummarizatorKind getKind() {
        return SummarizatorKind.FULL_DOCUMENT;
    }
}
