package org.jabref.logic.ai.models;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jabref.model.ai.AiProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model provider for OpenAI-compatible APIs.
 * Fetches available models from the /v1/models endpoint.
 */
public class OpenAiCompatibleModelProvider implements AiModelProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiCompatibleModelProvider.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public OpenAiCompatibleModelProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public OpenAiCompatibleModelProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public List<String> fetchModels(AiProvider aiProvider, String apiBaseUrl, String apiKey) {
        List<String> models = new ArrayList<>();

        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.debug("API key is not provided for {}, skipping model fetch", aiProvider.getLabel());
            return models;
        }

        try {
            String modelsEndpoint = buildModelsEndpoint(apiBaseUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelsEndpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                models = parseModelsFromResponse(response.body());
                LOGGER.info("Successfully fetched {} models from {}", models.size(), aiProvider.getLabel());
            } else {
                LOGGER.debug("Failed to fetch models from {} (status: {})", aiProvider.getLabel(), response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Failed to fetch models from {}: {}", aiProvider.getLabel(), e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            LOGGER.debug("Unexpected error while fetching models from {}: {}", aiProvider.getLabel(), e.getMessage());
        }

        return models;
    }

    @Override
    public boolean supports(AiProvider aiProvider) {
        // OpenAI-compatible providers: OpenAI, Mistral AI, and custom OpenAI-compatible endpoints
        return aiProvider == AiProvider.OPEN_AI
                || aiProvider == AiProvider.MISTRAL_AI
                || aiProvider == AiProvider.GPT4ALL;
    }

    private String buildModelsEndpoint(String apiBaseUrl) {
        String baseUrl = apiBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/models";
        } else {
            return baseUrl + "/v1/models";
        }
    }

    private List<String> parseModelsFromResponse(String responseBody) {
        List<String> models = new ArrayList<>();

        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonResponse.has("data") && jsonResponse.get("data").isJsonArray()) {
                JsonArray modelsArray = jsonResponse.getAsJsonArray("data");

                for (JsonElement element : modelsArray) {
                    if (element.isJsonObject()) {
                        JsonObject modelObject = element.getAsJsonObject();
                        if (modelObject.has("id")) {
                            String modelId = modelObject.get("id").getAsString();
                            models.add(modelId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse models response: {}", e.getMessage());
        }

        return models;
    }
}
