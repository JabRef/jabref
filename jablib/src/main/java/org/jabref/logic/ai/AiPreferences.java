package org.jabref.logic.ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.model.ai.AiProvider;
import org.jabref.model.ai.EmbeddingModel;
import org.jabref.model.strings.StringUtil;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiPreferences.class);

    private static final String KEYRING_AI_SERVICE = "org.jabref.ai";
    private static final String KEYRING_AI_SERVICE_ACCOUNT = "apiKey";

    private final BooleanProperty enableAi;
    private final BooleanProperty autoGenerateEmbeddings;
    private final BooleanProperty autoGenerateSummaries;

    private final ObjectProperty<AiProvider> aiProvider;

    private final StringProperty openAiChatModel;
    private final StringProperty mistralAiChatModel;
    private final StringProperty geminiChatModel;
    private final StringProperty huggingFaceChatModel;
    private final StringProperty gpt4AllChatModel;

    private final BooleanProperty customizeExpertSettings;

    private final StringProperty openAiApiBaseUrl;
    private final StringProperty mistralAiApiBaseUrl;
    private final StringProperty geminiApiBaseUrl;
    private final StringProperty huggingFaceApiBaseUrl;
    private final StringProperty gpt4AllApiBaseUrl;

    private final ObjectProperty<EmbeddingModel> embeddingModel;
    private final DoubleProperty temperature;
    private final IntegerProperty contextWindowSize;
    private final IntegerProperty documentSplitterChunkSize;
    private final IntegerProperty documentSplitterOverlapSize;
    private final IntegerProperty ragMaxResultsCount;
    private final DoubleProperty ragMinScore;

    private final Map<AiTemplate, StringProperty> templates;

    private Runnable apiKeyChangeListener;

    public AiPreferences(boolean enableAi,
                         boolean autoGenerateEmbeddings,
                         boolean autoGenerateSummaries,
                         AiProvider aiProvider,
                         String openAiChatModel,
                         String mistralAiChatModel,
                         String geminiChatModel,
                         String huggingFaceChatModel,
                         String gpt4AllModel,
                         boolean customizeExpertSettings,
                         String openAiApiBaseUrl,
                         String mistralAiApiBaseUrl,
                         String geminiApiBaseUrl,
                         String huggingFaceApiBaseUrl,
                         String gpt4AllApiBaseUrl,
                         EmbeddingModel embeddingModel,
                         double temperature,
                         int contextWindowSize,
                         int documentSplitterChunkSize,
                         int documentSplitterOverlapSize,
                         int ragMaxResultsCount,
                         double ragMinScore,
                         Map<AiTemplate, String> templates
    ) {
        this.enableAi = new SimpleBooleanProperty(enableAi);
        this.autoGenerateEmbeddings = new SimpleBooleanProperty(autoGenerateEmbeddings);
        this.autoGenerateSummaries = new SimpleBooleanProperty(autoGenerateSummaries);

        this.aiProvider = new SimpleObjectProperty<>(aiProvider);

        this.openAiChatModel = new SimpleStringProperty(openAiChatModel);
        this.mistralAiChatModel = new SimpleStringProperty(mistralAiChatModel);
        this.geminiChatModel = new SimpleStringProperty(geminiChatModel);
        this.huggingFaceChatModel = new SimpleStringProperty(huggingFaceChatModel);
        this.gpt4AllChatModel = new SimpleStringProperty(gpt4AllModel);

        this.customizeExpertSettings = new SimpleBooleanProperty(customizeExpertSettings);

        this.openAiApiBaseUrl = new SimpleStringProperty(openAiApiBaseUrl);
        this.mistralAiApiBaseUrl = new SimpleStringProperty(mistralAiApiBaseUrl);
        this.geminiApiBaseUrl = new SimpleStringProperty(geminiApiBaseUrl);
        this.huggingFaceApiBaseUrl = new SimpleStringProperty(huggingFaceApiBaseUrl);
        this.gpt4AllApiBaseUrl = new SimpleStringProperty(gpt4AllApiBaseUrl);

        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
        this.temperature = new SimpleDoubleProperty(temperature);
        this.contextWindowSize = new SimpleIntegerProperty(contextWindowSize);
        this.documentSplitterChunkSize = new SimpleIntegerProperty(documentSplitterChunkSize);
        this.documentSplitterOverlapSize = new SimpleIntegerProperty(documentSplitterOverlapSize);
        this.ragMaxResultsCount = new SimpleIntegerProperty(ragMaxResultsCount);
        this.ragMinScore = new SimpleDoubleProperty(ragMinScore);

        this.templates = Map.of(
                AiTemplate.CHATTING_SYSTEM_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.CHATTING_SYSTEM_MESSAGE)),
                AiTemplate.CHATTING_USER_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.CHATTING_USER_MESSAGE)),
                AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE)),
                AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE)),
                AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE)),
                AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE)),
                AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE)),
                AiTemplate.CITATION_PARSING_USER_MESSAGE, new SimpleStringProperty(templates.get(AiTemplate.CITATION_PARSING_USER_MESSAGE))
        );
    }

    public String getApiKeyForAiProvider(AiProvider aiProvider) {
        try (final Keyring keyring = Keyring.create()) {
            return keyring.getPassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + aiProvider.name());
        } catch (PasswordAccessException e) {
            LOGGER.debug("No API key stored for provider {}. Returning an empty string", aiProvider.getLabel());
            return "";
        } catch (Exception e) {
            LOGGER.warn("JabRef could not open keyring for retrieving {} API token", aiProvider.getLabel(), e);
            return "";
        }
    }

    public void storeAiApiKeyInKeyring(AiProvider aiProvider, String newKey) {
        try (final Keyring keyring = Keyring.create()) {
            if (StringUtil.isNullOrEmpty(newKey)) {
                try {
                    keyring.deletePassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + aiProvider.name());
                } catch (PasswordAccessException ex) {
                    LOGGER.debug("API key for provider {} not stored in keyring. JabRef does not store an empty key.", aiProvider.getLabel());
                }
            } else {
                keyring.setPassword(KEYRING_AI_SERVICE, KEYRING_AI_SERVICE_ACCOUNT + "-" + aiProvider.name(), newKey);
            }
        } catch (Exception e) {
            LOGGER.warn("JabRef could not open keyring for storing {} API token", aiProvider.getLabel(), e);
        }
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

    public BooleanProperty autoGenerateEmbeddingsProperty() {
        return autoGenerateEmbeddings;
    }

    public boolean getAutoGenerateEmbeddings() {
        return autoGenerateEmbeddings.get();
    }

    public void setAutoGenerateEmbeddings(boolean autoGenerateEmbeddings) {
        this.autoGenerateEmbeddings.set(autoGenerateEmbeddings);
    }

    public BooleanProperty autoGenerateSummariesProperty() {
        return autoGenerateSummaries;
    }

    public boolean getAutoGenerateSummaries() {
        return autoGenerateSummaries.get();
    }

    public void setAutoGenerateSummaries(boolean autoGenerateSummaries) {
        this.autoGenerateSummaries.set(autoGenerateSummaries);
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

    public StringProperty geminiChatModelProperty() {
        return geminiChatModel;
    }

    public String getGeminiChatModel() {
        return geminiChatModel.get();
    }

    public void setGeminiChatModel(String geminiChatModel) {
        this.geminiChatModel.set(geminiChatModel);
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

    public StringProperty gpt4AllChatModelProperty() {
        return gpt4AllChatModel;
    }

    public String getGpt4AllChatModel() {
        return gpt4AllChatModel.get();
    }

    public void setGpt4AllChatModel(String gpt4AllChatModel) {
        this.gpt4AllChatModel.set(gpt4AllChatModel);
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

    public StringProperty geminiApiBaseUrlProperty() {
        return geminiApiBaseUrl;
    }

    public String getGeminiApiBaseUrl() {
        return geminiApiBaseUrl.get();
    }

    public void setGeminiApiBaseUrl(String geminiApiBaseUrl) {
        this.geminiApiBaseUrl.set(geminiApiBaseUrl);
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

    public StringProperty gpt4AllApiBaseUrlProperty() {
        return gpt4AllApiBaseUrl;
    }

    public String getGpt4AllApiBaseUrl() {
        return gpt4AllApiBaseUrl.get();
    }

    public void setGpt4AllApiBaseUrl(String gpt4AllApiBaseUrl) {
        this.gpt4AllApiBaseUrl.set(gpt4AllApiBaseUrl);
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
                case GEMINI -> AiDefaultPreferences.getContextWindowSize(AiProvider.GEMINI, geminiChatModel.get());
                case GPT4ALL -> AiDefaultPreferences.getContextWindowSize(AiProvider.GPT4ALL, gpt4AllChatModel.get());
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
        List<Property<?>> observables = List.of(openAiChatModel, mistralAiChatModel, huggingFaceChatModel);

        observables.forEach(obs -> obs.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        }));
    }

    public void addListenerToApiBaseUrls(Runnable runnable) {
        List<Property<?>> observables = List.of(openAiApiBaseUrl, mistralAiApiBaseUrl, huggingFaceApiBaseUrl);

        observables.forEach(obs -> obs.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        }));
    }

    public String getSelectedChatModel() {
        return switch (aiProvider.get()) {
            case OPEN_AI ->
                    openAiChatModel.get();
            case MISTRAL_AI ->
                    mistralAiChatModel.get();
            case HUGGING_FACE ->
                    huggingFaceChatModel.get();
            case GEMINI ->
                    geminiChatModel.get();
            case GPT4ALL ->
                    gpt4AllChatModel.get();
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
                case GEMINI ->
                        geminiApiBaseUrl.get();
                case GPT4ALL ->
                        gpt4AllApiBaseUrl.get();
            };
        } else {
            return aiProvider.get().getApiUrl();
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

    public void setTemplate(AiTemplate aiTemplate, String template) {
        templates.get(aiTemplate).set(template);
    }

    public String getTemplate(AiTemplate aiTemplate) {
        return templates.get(aiTemplate).get();
    }

    public StringProperty templateProperty(AiTemplate aiTemplate) {
        return templates.get(aiTemplate);
    }
}
