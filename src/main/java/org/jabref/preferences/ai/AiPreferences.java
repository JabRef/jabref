package org.jabref.preferences.ai;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.ai.AiDefaultPreferences;
import org.jabref.preferences.PreferencesService;

public class AiPreferences {
    private final PreferencesService preferencesService;

    private final BooleanProperty enableAi;

    private final ObjectProperty<AiProvider> aiProvider;

    private final StringProperty openAiChatModel;
    private final StringProperty mistralAiChatModel;
    private final StringProperty huggingFaceChatModel;

    private final BooleanProperty customizeExpertSettings;

    private final StringProperty openAiApiBaseUrl;
    private final StringProperty mistralAiApiBaseUrl;
    private final StringProperty huggingFaceApiBaseUrl;

    private final ObjectProperty<EmbeddingModel> embeddingModel;
    private final StringProperty instruction;
    private final DoubleProperty temperature;
    private final IntegerProperty contextWindowSize;
    private final IntegerProperty documentSplitterChunkSize;
    private final IntegerProperty documentSplitterOverlapSize;
    private final IntegerProperty ragMaxResultsCount;
    private final DoubleProperty ragMinScore;

    private Runnable apiKeyChangeListener;

    public AiPreferences(PreferencesService preferencesService,
                         boolean enableAi,
                         AiProvider aiProvider,
                         String openAiChatModel,
                         String mistralAiChatModel,
                         String huggingFaceChatModel,
                         boolean customizeExpertSettings,
                         String openAiApiBaseUrl,
                         String mistralAiApiBaseUrl,
                         String huggingFaceApiBaseUrl,
                         EmbeddingModel embeddingModel,
                         String instruction,
                         double temperature,
                         int contextWindowSize,
                         int documentSplitterChunkSize,
                         int documentSplitterOverlapSize,
                         int ragMaxResultsCount,
                         double ragMinScore
    ) {
        this.preferencesService = preferencesService;

        this.enableAi = new SimpleBooleanProperty(enableAi);

        this.aiProvider = new SimpleObjectProperty<>(aiProvider);

        this.openAiChatModel = new SimpleStringProperty(openAiChatModel);
        this.mistralAiChatModel = new SimpleStringProperty(mistralAiChatModel);
        this.huggingFaceChatModel = new SimpleStringProperty(huggingFaceChatModel);

        this.customizeExpertSettings = new SimpleBooleanProperty(customizeExpertSettings);

        this.openAiApiBaseUrl = new SimpleStringProperty(openAiApiBaseUrl);
        this.mistralAiApiBaseUrl = new SimpleStringProperty(mistralAiApiBaseUrl);
        this.huggingFaceApiBaseUrl = new SimpleStringProperty(huggingFaceApiBaseUrl);

        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
        this.instruction = new SimpleStringProperty(instruction);
        this.temperature = new SimpleDoubleProperty(temperature);
        this.contextWindowSize = new SimpleIntegerProperty(contextWindowSize);
        this.documentSplitterChunkSize = new SimpleIntegerProperty(documentSplitterChunkSize);
        this.documentSplitterOverlapSize = new SimpleIntegerProperty(documentSplitterOverlapSize);
        this.ragMaxResultsCount = new SimpleIntegerProperty(ragMaxResultsCount);
        this.ragMinScore = new SimpleDoubleProperty(ragMinScore);
    }

    public BooleanProperty enableAiProperty() {
        return enableAi;
    }

    public boolean getEnableAi() {
        return enableAi.get();
    }

    public void setEnableAi(boolean enableAi) {
        this.enableAi.set(enableAi);
    }

    public ObjectProperty<AiProvider> aiProviderProperty() {
        return aiProvider;
    }

    public AiProvider getAiProvider() {
        return aiProvider.get();
    }

    public void setAiProvider(AiProvider aiProvider) {
        this.aiProvider.set(aiProvider);
    }

    public StringProperty openAiChatModelProperty() {
        return openAiChatModel;
    }

    public String getOpenAiChatModel() {
        return openAiChatModel.get();
    }

    public void setOpenAiChatModel(String openAiChatModel) {
        this.openAiChatModel.set(openAiChatModel);
    }

    public StringProperty mistralAiChatModelProperty() {
        return mistralAiChatModel;
    }

    public String getMistralAiChatModel() {
        return mistralAiChatModel.get();
    }

    public void setMistralAiChatModel(String mistralAiChatModel) {
        this.mistralAiChatModel.set(mistralAiChatModel);
    }

    public StringProperty huggingFaceChatModelProperty() {
        return huggingFaceChatModel;
    }

    public String getHuggingFaceChatModel() {
        return huggingFaceChatModel.get();
    }

    public void setHuggingFaceChatModel(String huggingFaceChatModel) {
        this.huggingFaceChatModel.set(huggingFaceChatModel);
    }

    public BooleanProperty customizeExpertSettingsProperty() {
        return customizeExpertSettings;
    }

    public boolean getCustomizeExpertSettings() {
        return customizeExpertSettings.get();
    }

    public void setCustomizeExpertSettings(boolean customizeExpertSettings) {
        this.customizeExpertSettings.set(customizeExpertSettings);
    }

    public ObjectProperty<EmbeddingModel> embeddingModelProperty() {
        return embeddingModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        if (getCustomizeExpertSettings()) {
            return embeddingModel.get();
        } else {
            return AiDefaultPreferences.EMBEDDING_MODEL;
        }
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel.set(embeddingModel);
    }

    public StringProperty openAiApiBaseUrlProperty() {
        return openAiApiBaseUrl;
    }

    public String getOpenAiApiBaseUrl() {
        return openAiApiBaseUrl.get();
    }

    public void setOpenAiApiBaseUrl(String openAiApiBaseUrl) {
        this.openAiApiBaseUrl.set(openAiApiBaseUrl);
    }

    public StringProperty mistralAiApiBaseUrlProperty() {
        return mistralAiApiBaseUrl;
    }

    public String getMistralAiApiBaseUrl() {
        return mistralAiApiBaseUrl.get();
    }

    public void setMistralAiApiBaseUrl(String mistralAiApiBaseUrl) {
        this.mistralAiApiBaseUrl.set(mistralAiApiBaseUrl);
    }

    public StringProperty huggingFaceApiBaseUrlProperty() {
        return huggingFaceApiBaseUrl;
    }

    public String getHuggingFaceApiBaseUrl() {
        return huggingFaceApiBaseUrl.get();
    }

    public void setHuggingFaceApiBaseUrl(String huggingFaceApiBaseUrl) {
        this.huggingFaceApiBaseUrl.set(huggingFaceApiBaseUrl);
    }

    public StringProperty instructionProperty() {
        return instruction;
    }

    public String getInstruction() {
        if (getCustomizeExpertSettings()) {
            return instruction.get();
        } else {
            return AiDefaultPreferences.SYSTEM_MESSAGE;
        }
    }

    public void setInstruction(String instruction) {
        this.instruction.set(instruction);
    }

    public DoubleProperty temperatureProperty() {
        return temperature;
    }

    public double getTemperature() {
        if (getCustomizeExpertSettings()) {
            return temperature.get();
        } else {
            return AiDefaultPreferences.TEMPERATURE;
        }
    }

    public void setTemperature(double temperature) {
        this.temperature.set(temperature);
    }

    public IntegerProperty contextWindowSizeProperty() {
        return contextWindowSize;
    }

    public int getContextWindowSize() {
        if (getCustomizeExpertSettings()) {
            return contextWindowSize.get();
        } else {
            return switch (aiProvider.get()) {
                case OPEN_AI -> AiDefaultPreferences.getContextWindowSize(AiProvider.OPEN_AI, openAiChatModel.get());
                case MISTRAL_AI -> AiDefaultPreferences.getContextWindowSize(AiProvider.MISTRAL_AI, mistralAiChatModel.get());
                case HUGGING_FACE -> AiDefaultPreferences.getContextWindowSize(AiProvider.HUGGING_FACE, huggingFaceChatModel.get());
            };
        }
    }

    public void setContextWindowSize(int contextWindowSize) {
        this.contextWindowSize.set(contextWindowSize);
    }

    public IntegerProperty documentSplitterChunkSizeProperty() {
        return documentSplitterChunkSize;
    }

    public int getDocumentSplitterChunkSize() {
        if (getCustomizeExpertSettings()) {
            return documentSplitterChunkSize.get();
        } else {
            return AiDefaultPreferences.DOCUMENT_SPLITTER_CHUNK_SIZE;
        }
    }

    public void setDocumentSplitterChunkSize(int documentSplitterChunkSize) {
        this.documentSplitterChunkSize.set(documentSplitterChunkSize);
    }

    public IntegerProperty documentSplitterOverlapSizeProperty() {
        return documentSplitterOverlapSize;
    }

    public int getDocumentSplitterOverlapSize() {
        if (getCustomizeExpertSettings()) {
            return documentSplitterOverlapSize.get();
        } else {
            return AiDefaultPreferences.DOCUMENT_SPLITTER_OVERLAP;
        }
    }

    public void setDocumentSplitterOverlapSize(int documentSplitterOverlapSize) {
        this.documentSplitterOverlapSize.set(documentSplitterOverlapSize);
    }

    public IntegerProperty ragMaxResultsCountProperty() {
        return ragMaxResultsCount;
    }

    public int getRagMaxResultsCount() {
        if (getCustomizeExpertSettings()) {
            return ragMaxResultsCount.get();
        } else {
            return AiDefaultPreferences.RAG_MAX_RESULTS_COUNT;
        }
    }

    public void setRagMaxResultsCount(int ragMaxResultsCount) {
        this.ragMaxResultsCount.set(ragMaxResultsCount);
    }

    public DoubleProperty ragMinScoreProperty() {
        return ragMinScore;
    }

    public double getRagMinScore() {
        if (getCustomizeExpertSettings()) {
            return ragMinScore.get();
        } else {
            return AiDefaultPreferences.RAG_MIN_SCORE;
        }
    }

    public void setRagMinScore(double ragMinScore) {
        this.ragMinScore.set(ragMinScore);
    }

    /**
     * Listen to changes of preferences that are related to embeddings generation.
     *
     * @param runnable The runnable that should be executed when the preferences change.
     */
    public void addListenerToEmbeddingsParametersChange(Runnable runnable) {
        embeddingModel.addListener((observableValue, oldValue, newValue) -> {
            if (newValue != oldValue) {
                runnable.run();
            }
        });

        documentSplitterChunkSize.addListener((observableValue, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                runnable.run();
            }
        });

        documentSplitterOverlapSize.addListener((observableValue, oldValue, newValue) -> {
            if (!Objects.equals(newValue, oldValue)) {
                runnable.run();
            }
        });
    }

    public void addListenerToChatModels(Runnable runnable) {
        openAiChatModel.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        });

        mistralAiChatModel.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        });

        huggingFaceChatModel.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        });
    }

    public void addListenerToApiBaseUrls(Runnable runnable) {
        openAiApiBaseUrl.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        });

        mistralAiApiBaseUrl.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        });

        huggingFaceApiBaseUrl.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        });
    }

    public String getSelectedChatModel() {
        return switch (aiProvider.get()) {
            case OPEN_AI ->
                    openAiChatModel.get();
            case MISTRAL_AI ->
                    mistralAiChatModel.get();
            case HUGGING_FACE ->
                    huggingFaceChatModel.get();
        };
    }

    public String getSelectedApiBaseUrl() {
        if (customizeExpertSettings.get()) {
            return switch (aiProvider.get()) {
                case OPEN_AI ->
                        openAiApiBaseUrl.get();
                case MISTRAL_AI ->
                        mistralAiApiBaseUrl.get();
                case HUGGING_FACE ->
                        huggingFaceApiBaseUrl.get();
            };
        } else {
            return AiDefaultPreferences.PROVIDERS_API_URLS.get(aiProvider.get());
        }
    }

    public void setApiKeyChangeListener(Runnable apiKeyChangeListener) {
        this.apiKeyChangeListener = apiKeyChangeListener;
    }

    /**
     * Notify that the API key has been updated.
     */
    public void apiKeyUpdated() {
        apiKeyChangeListener.run();
    }
}
