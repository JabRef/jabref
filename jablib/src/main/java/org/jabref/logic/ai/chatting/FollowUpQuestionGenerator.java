package org.jabref.logic.ai.chatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.templates.AiTemplatesService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FollowUpQuestionGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowUpQuestionGenerator.class);
    private static final int MIN_QUESTION_LENGTH = 5;
    private static final int MAX_QUESTION_LENGTH = 100;

    private final ChatModel chatLanguageModel;
    private final AiTemplatesService aiTemplatesService;
    private final AiPreferences aiPreferences;

    public FollowUpQuestionGenerator(ChatModel chatLanguageModel, AiTemplatesService aiTemplatesService, AiPreferences aiPreferences) {
        this.chatLanguageModel = chatLanguageModel;
        this.aiTemplatesService = aiTemplatesService;
        this.aiPreferences = aiPreferences;
    }

    public List<String> generateFollowUpQuestions(UserMessage userMessage, AiMessage aiMessage) {
        try {
            String prompt = buildPrompt(userMessage.singleText(), aiMessage.text());

            LOGGER.debug("Generating follow-up questions for conversation");

            AiMessage response = chatLanguageModel.chat(List.of(new UserMessage(prompt))).aiMessage();
            String responseText = response.text();

            LOGGER.debug("Received follow-up questions response: {}", responseText);

            List<String> questions = parseQuestions(responseText);

            LOGGER.info("Generated {} follow-up questions", questions.size());

            return questions;
        } catch (Exception e) {
            LOGGER.warn("Failed to generate follow-up questions", e);
            return new ArrayList<>();
        }
    }

    private String buildPrompt(String userMessage, String aiResponse) {
        return aiTemplatesService.makeFollowUpQuestionsPrompt(userMessage, aiResponse, aiPreferences.getFollowUpQuestionsCount());
    }

    private List<String> parseQuestions(String response) {
        List<String> questions = new ArrayList<>();

        Pattern numberedPattern = Pattern.compile("^\\s*\\d+\\.\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = numberedPattern.matcher(response);

        while (matcher.find() && questions.size() < aiPreferences.getFollowUpQuestionsCount()) {
            String question = matcher.group(1).trim();

            question = question.replaceAll("^[\"']|[\"']$", "");

            if (isValidQuestion(question)) {
                questions.add(question);
            }
        }

        if (questions.isEmpty()) {
            LOGGER.debug("Numbered format parsing failed, trying line-by-line parsing");
            String[] lines = response.split("\n");

            for (String line : lines) {
                if (questions.size() >= aiPreferences.getFollowUpQuestionsCount()) {
                    break;
                }

                line = line.trim()
                           .replaceAll("^[-*•]\\s*", "")
                           .replaceAll("^\\d+\\.\\s*", "")
                           .replaceAll("^[\"']|[\"']$", "");

                if (isValidQuestion(line)) {
                    questions.add(line);
                }
            }
        }

        return questions;
    }

    private boolean isValidQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }

        int length = question.length();
        if (length < MIN_QUESTION_LENGTH || length > MAX_QUESTION_LENGTH) {
            LOGGER.debug("Question length {} is outside valid range [{}, {}]", length, MIN_QUESTION_LENGTH, MAX_QUESTION_LENGTH);
            return false;
        }

        return true;
    }
}
