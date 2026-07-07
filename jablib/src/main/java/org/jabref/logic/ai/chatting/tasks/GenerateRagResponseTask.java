package org.jabref.logic.ai.chatting.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatHistoryUtils;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.RelevantInformation;

import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The task responsible for generating a RAG (retrieval-augmented generation) response. Before sending a user message to the LLM, the [AnswerEngine] is called which finds the relevant context for the message.
public class GenerateRagResponseTask extends BackgroundTask<ChatMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateRagResponseTask.class);

    private final ChatModel chatModel;
    private final AnswerEngine answerEngine;
    private final List<ChatMessage> chatHistory;
    private final List<FullBibEntry> entries;
    private final String systemMessageTemplate;
    private final String injectionTemplate;

    public GenerateRagResponseTask(
            ChatModel chatModel,
            AnswerEngine answerEngine,
            List<ChatMessage> chatHistory,
            List<FullBibEntry> entries,
            String systemMessageTemplate,
            String injectionTemplate
    ) {
        this.chatModel = chatModel;
        this.answerEngine = answerEngine;
        this.chatHistory = chatHistory;
        this.entries = entries;
        this.systemMessageTemplate = systemMessageTemplate;
        this.injectionTemplate = injectionTemplate;

        showToUser(true);
        titleProperty().set(Localization.lang("Waiting for AI reply..."));
    }

    @Override
    public ChatMessage call() throws Exception {
        List<ChatMessage> workingChatHistory = new ArrayList<>(chatHistory);

        Optional<ChatMessage> userMessage = ChatHistoryUtils.getLastUserMessage(workingChatHistory);

        if (userMessage.isEmpty()) {
            LOGGER.error("A chat history without a user message at the end was sent to GenerateRagResponseTask. This should not happen, returning an empty AI message.");
            return ChatMessage.aiMessage("", List.of());
        }

        List<RelevantInformation> relevantInformation = answerEngine.process(
                userMessage.get().content(),
                entries
        );

        String injected = AiTemplateRenderer.renderChattingUserMessage(
                injectionTemplate,
                entries.stream().map(FullBibEntry::entry).toList(),
                userMessage.get().content(),
                relevantInformation
        );

        ChatMessage injectedMessage = ChatMessage.userMessage(userMessage.get().timestamp(), injected);

        ChatMessage systemMessage = ChatMessage.systemMessage(AiTemplateRenderer.renderChattingSystemMessage(
                systemMessageTemplate,
                entries.stream().map(FullBibEntry::entry).toList()
        ));

        List<ChatMessage> chatHistoryForLlm = new ArrayList<>();
        chatHistoryForLlm.add(systemMessage);
        chatHistoryForLlm.addAll(workingChatHistory);

        if (chatHistoryForLlm.getLast().role() != ChatMessage.Role.SYSTEM) {
            chatHistoryForLlm.removeLast();
        }
        // [impl->req~ai.chat.uses-answer-engine~1]
        chatHistoryForLlm.add(injectedMessage);

        List<dev.langchain4j.data.message.ChatMessage> chatMessages = chatHistoryForLlm
                .stream()
                .map(ChatMessage::toLangChainMessage)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        ChatResponse response = chatModel.chat(chatMessages);
        String content = response.aiMessage().text();

        return ChatMessage.aiMessage(
                content,
                relevantInformation
        );
    }
}
