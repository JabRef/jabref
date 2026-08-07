package org.jabref.logic.ai.followup.tasks;

import java.util.List;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.logic.ai.util.LlmResponseParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;

import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateFollowUpQuestions extends BackgroundTask<List<String>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateFollowUpQuestions.class);

    private final ChatModel chatModel;
    private final AiPreferences aiPreferences;
    private final String userMessage;
    private final String aiResponse;

    public GenerateFollowUpQuestions(
            ChatModel chatModel,
            AiPreferences aiPreferences,
            String userMessage,
            String aiResponse
    ) {
        this.chatModel = chatModel;
        this.aiPreferences = aiPreferences;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;

        titleProperty().set(Localization.lang("Generating follow-up questions..."));
    }

    @Override
    public List<String> call() throws Exception {
        String prompt = AiTemplateRenderer.renderFollowUpQuestionsPrompt(
                aiPreferences.getFollowUpQuestionsTemplate(),
                userMessage,
                aiResponse,
                aiPreferences.getFollowUpQuestionsCount()
        );

        LOGGER.debug("Generating follow-up questions for conversation");

        String responseText = chatModel.chat(List.of(new UserMessage(prompt))).aiMessage().text();

        LOGGER.debug("Received follow-up questions response: {}", responseText);

        List<String> questions = LlmResponseParser.extractNumberedList(responseText)
                                                  .stream()
                                                  .limit(aiPreferences.getFollowUpQuestionsCount())
                                                  .toList();

        LOGGER.debug("Generated {} follow-up questions", questions.size());

        return questions.subList(0, Math.min(questions.size(), aiPreferences.getFollowUpQuestionsCount()));
    }
}
