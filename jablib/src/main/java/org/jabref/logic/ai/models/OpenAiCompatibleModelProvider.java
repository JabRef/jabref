package org.jabref.logic.ai.models;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.ai.AiProvider;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model provider for OpenAI-compatible APIs.
 * Fetches available models from the /v1/models endpoint.
 * Mistral provides an OpenAI-compatible API, so this works for Mistral as well.
 */
public class OpenAiCompatibleModelProvider implements AiModelProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiCompatibleModelProvider.class);

    @Override
    public List<String> fetchModels(AiProvider aiProvider, String apiBaseUrl, String apiKey) {
        List<String> models = new ArrayList<>();

        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.debug("API key is not provided for {}, skipping model fetch", aiProvider.getLabel());
            return models;
        }

        try {
            String modelsEndpoint = buildModelsEndpoint(apiBaseUrl);
            HttpResponse<JsonNode> response = Unirest.get(modelsEndpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("accept", "application/json")
                    .asJson();

            if (response.getStatus() == 200 && response.getBody() != null) {
                models = parseModelsFromResponse(response.getBody());
                LOGGER.info("Successfully fetched {} models from {}", models.size(), aiProvider.getLabel());
            } else {
                LOGGER.debug("Failed to fetch models from {} (status: {})", aiProvider.getLabel(), response.getStatus());
            }
        } catch (UnirestException e) {
            LOGGER.debug("Failed to fetch models from {}: {}", aiProvider.getLabel(), e.getMessage());
        } catch (Exception e) {
            LOGGER.debug("Unexpected error while fetching models from {}: {}", aiProvider.getLabel(), e.getMessage());
        }

        return models;
    }

    @Override
    public boolean supports(AiProvider aiProvider) {
        return aiProvider == AiProvider.OPEN_AI
                || aiProvider == AiProvider.MISTRAL_AI
                || aiProvider == AiProvider.GPT4ALL;
    }

    /**
     * Builds the URL for the models endpoint from the given API base URL.
     * <p>
     * The OpenAI API specification defines the models endpoint at /v1/models.
     * This method handles various URL formats:
     * <ul>
     *   <li>If the URL already ends with /v1, appends /models</li>
     *   <li>If the URL doesn't end with /v1, appends /v1/models</li>
     *   <li>Removes trailing slashes before building the path</li>
     * </ul>
     *
     * @param apiBaseUrl the base URL of the API (e.g., "https://api.openai.com" or "https://api.openai.com/v1")
     * @return the complete URL for the models endpoint
     */
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

    private List<String> parseModelsFromResponse(JsonNode jsonNode) {
        List<String> models = new ArrayList<>();

        try {
            JSONObject jsonResponse = jsonNode.getObject();

            if (jsonResponse.has("data")) {
                JSONArray modelsArray = jsonResponse.getJSONArray("data");

                for (int i = 0; i < modelsArray.length(); i++) {
                    JSONObject modelObject = modelsArray.getJSONObject(i);
                    if (modelObject.has("id")) {
                        String modelId = modelObject.getString("id");
                        models.add(modelId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse models response.", e);
        }

        return models;
    }
}
