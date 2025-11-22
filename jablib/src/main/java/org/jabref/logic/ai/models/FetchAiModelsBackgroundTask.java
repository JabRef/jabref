package org.jabref.logic.ai.models;

import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.ai.AiProvider;

/**
 * Background task for fetching AI models from a provider's API.
 */
public class FetchAiModelsBackgroundTask extends BackgroundTask<List<String>> {

    private final AiModelService aiModelService;
    private final AiProvider aiProvider;
    private final String apiBaseUrl;
    private final String apiKey;

    public FetchAiModelsBackgroundTask(AiModelService aiModelService, AiProvider aiProvider, String apiBaseUrl, String apiKey) {
        this.aiModelService = aiModelService;
        this.aiProvider = aiProvider;
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;

        configure();
    }

    private void configure() {
        showToUser(false);
        titleProperty().set(Localization.lang("Fetching models for %0", aiProvider.getLabel()));
        willBeRecoveredAutomatically(true);
    }

    @Override
    public List<String> call() {
        return aiModelService.fetchModelsSynchronously(
                aiProvider,
                apiBaseUrl,
                apiKey
        );
    }
}
