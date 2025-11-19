package org.jabref.logic.ai.chatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FollowUpQuestionGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FollowUpQuestionGenerator.class);
    private static final int MAX_QUESTIONS = 5;
    private static final int MIN_QUESTION_LENGTH = 5;
    private static final int MAX_QUESTION_LENGTH = 100;

    private final ChatModel chatLanguageModel;

    public FollowUpQuestionGenerator(ChatModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
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
            return new ArrayList<>(); // Return empty list on failure
        }
    }

    private String buildPrompt(String userMessage, String aiResponse) {
        return String.format("""
                  Based on this conversation:

                  User: %s
                  Assistant: %s

                  Generate 3-5 short follow-up questions (maximum 10 words each) that the user might want to ask next.
                  Format your response as a numbered list:
                  1. [question]
                  2. [question]
                  3. [question]

                  Only provide the numbered list, nothing else.""",
                userMessage, aiResponse);
    }

    private List<String> parseQuestions(String response) {
        List<String> questions = new ArrayList<>();

        // Pattern to match numbered list items: "1. Question text"
        Pattern numberedPattern = Pattern.compile("^\\s*\\d+\\.\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = numberedPattern.matcher(response);

        while (matcher.find() && questions.size() < MAX_QUESTIONS) {
            String question = matcher.group(1).trim();

            // Remove quotes if present
            question = question.replaceAll("^[\"']|[\"']$", "");

            // Validate question
            if (isValidQuestion(question)) {
                questions.add(question);
            }
        }

        // If numbered format didn't work, try splitting by newlines and filtering
        if (questions.isEmpty()) {
            LOGGER.debug("Numbered format parsing failed, trying line-by-line parsing");
            String[] lines = response.split("\n");

            for (String line : lines) {
                if (questions.size() >= MAX_QUESTIONS) {
                    break;
                }

                line = line.trim();
                // Remove common prefixes
                line = line.replaceAll("^[-*•]\\s*", "");
                line = line.replaceAll("^\\d+\\.\\s*", "");
                line = line.replaceAll("^[\"']|[\"']$", "");

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
