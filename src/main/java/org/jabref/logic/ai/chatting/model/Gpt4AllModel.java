package org.jabref.logic.ai.chatting.model;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.util.URLUtil;

import com.google.gson.Gson;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gpt4AllModel implements ChatLanguageModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gpt4AllModel.class);

    private final AiPreferences aiPreferences;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public Gpt4AllModel(AiPreferences aiPreferences, HttpClient httpClient) {
        this.aiPreferences = aiPreferences;
        this.httpClient = httpClient;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> list) {
        LOGGER.debug("Generating response from Gpt4All model with {} messages: {}", list.size(), list);

        List<Message> messages = list.stream()
                .map(chatMessage -> switch (chatMessage) {
                    case AiMessage aiMessage -> new Message("assistant", aiMessage.text());
                    case SystemMessage systemMessage -> new Message("system", systemMessage.text());
                    case ToolExecutionResultMessage toolExecutionResultMessage -> new Message("tool", toolExecutionResultMessage.text());
                    case UserMessage userMessage -> new Message("user", userMessage.singleText());
                    default -> throw new IllegalStateException("Unknown ChatMessage type: " + chatMessage);
                }).collect(Collectors.toList());

        TextGenerationRequest request = TextGenerationRequest
                .builder()
                .model(aiPreferences.getSelectedChatModel())
                .messages(messages)
                .temperature(aiPreferences.getTemperature())
                .max_tokens(2048)
                .build();

        try {
            String requestBody = gson.toJson(request);
            String baseUrl = aiPreferences.getSelectedApiBaseUrl();
            String fullUrl = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URLUtil.createUri(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMinutes(1))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Gpt4All response: {}", response.body());

            TextGenerationResponse textGenerationResponse = gson.fromJson(response.body(), TextGenerationResponse.class);
            if (textGenerationResponse.choices() == null || textGenerationResponse.choices().isEmpty()) {
                throw new IllegalArgumentException("No choices returned in the response");
            }

            String generatedText = textGenerationResponse.choices().getFirst().message().content();
            if (generatedText == null || generatedText.isEmpty()) {
                throw new IllegalArgumentException("Generated text is null or empty");
            }

            // Note: We do not check the token usage and finish reason here.
            // This class is not a complete implementation of langchain4j's ChatLanguageModel.
            // We only implemented the functionality we specifically need.
            return new Response<>(new AiMessage(generatedText), new TokenUsage(0, 0), FinishReason.OTHER);
        } catch (Exception e) {
            LOGGER.error("Error generating message from Gpt4All", e);
            throw new RuntimeException("Failed to generate AI message", e);
        }
    }

    private static class TextGenerationRequest {
        protected final String model;
        protected final List<Message> messages;
        protected final Double temperature;
        protected final Integer max_tokens;

        private TextGenerationRequest(Builder builder) {
            this.model = builder.model;
            this.messages = builder.messages;
            this.temperature = builder.temperature;
            this.max_tokens = builder.max_tokens;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String model;
            private List<Message> messages;
            private Double temperature;
            private Integer max_tokens;

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder messages(List<Message> messages) {
                this.messages = messages;
                return this;
            }

            public Builder temperature(Double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder max_tokens(Integer max_tokens) {
                this.max_tokens = max_tokens;
                return this;
            }

            public TextGenerationRequest build() {
                return new TextGenerationRequest(this);
            }
        }
    }

    private record TextGenerationResponse(List<Choice> choices) { }

    private record Choice(Message message) { }

    private record Message(String role, String content) { }
}
