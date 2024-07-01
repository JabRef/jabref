package org.jabref.preferences;

import java.util.EnumMap;
import java.util.Map;
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

public class AiPreferences {
    public enum AiProvider {
        OPEN_AI("OpenAI"),
        MISTRAL_AI("Mistral AI");

        private final String name;

        AiProvider(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static AiProvider fromString(String text) {
            for (AiProvider b : AiProvider.values()) {
                if (b.name.equals(text)) {
                    return b;
                }
            }
            return OPEN_AI;
        }

        public String toString() {
            return name;
        }
    }

    public static final Map<AiProvider, String[]> CHAT_MODELS = new EnumMap<>(AiProvider.class);

    static {
        CHAT_MODELS.put(AiProvider.OPEN_AI, new String[]{"gpt-3.5-turbo", "gpt-4", "gpt-4-turbo", "gpt-4o"});
        CHAT_MODELS.put(AiProvider.MISTRAL_AI, new String[]{"open-mistral-7b", "open-mixtral-8x7b", "open-mixtral-8x22b", "mistral-small-latest", "mistral-medium-latest", "mistral-large-latest"});
    }

    public enum EmbeddingModel {
        ALL_MINLM_l6_V2("all-MiniLM-L6-v2"),
        ALL_MINLM_l6_V2_Q("all-MiniLM-L6-v2 (quantized)");

        private final String name;

        EmbeddingModel(String name) {
            this.name = name;
        }

        public static EmbeddingModel fromString(String text) {
            for (EmbeddingModel b : EmbeddingModel.values()) {
                if (b.name.equals(text)) {
                    return b;
                }
            }
            assert false;
            return null;
        }

        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }
    }

    private final BooleanProperty enableChatWithFiles;

    private final ObjectProperty<AiProvider> aiProvider;
    private final StringProperty chatModel;
    private final StringProperty apiToken;

    private final BooleanProperty customizeSettings;

    private final ObjectProperty<EmbeddingModel> embeddingModel;
    private final StringProperty systemMessage;
    private final DoubleProperty temperature;
    private final IntegerProperty messageWindowSize;
    private final IntegerProperty documentSplitterChunkSize;
    private final IntegerProperty documentSplitterOverlapSize;
    private final IntegerProperty ragMaxResultsCount;
    private final DoubleProperty ragMinScore;

    public AiPreferences(boolean enableChatWithFiles, AiProvider aiProvider, String chatModel, String apiToken, EmbeddingModel embeddingModel, boolean customizeSettings, String systemMessage, double temperature, int messageWindowSize, int documentSplitterChunkSize, int documentSplitterOverlapSize, int ragMaxResultsCount, double ragMinScore) {
        this.enableChatWithFiles = new SimpleBooleanProperty(enableChatWithFiles);

        this.aiProvider = new SimpleObjectProperty<>(aiProvider);
        this.chatModel = new SimpleStringProperty(chatModel);
        this.apiToken = new SimpleStringProperty(apiToken);

        this.customizeSettings = new SimpleBooleanProperty(customizeSettings);

        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
        this.systemMessage = new SimpleStringProperty(systemMessage);
        this.temperature = new SimpleDoubleProperty(temperature);
        this.messageWindowSize = new SimpleIntegerProperty(messageWindowSize);
        this.documentSplitterChunkSize = new SimpleIntegerProperty(documentSplitterChunkSize);
        this.documentSplitterOverlapSize = new SimpleIntegerProperty(documentSplitterOverlapSize);
        this.ragMaxResultsCount = new SimpleIntegerProperty(ragMaxResultsCount);
        this.ragMinScore = new SimpleDoubleProperty(ragMinScore);
    }

    public BooleanProperty enableChatWithFilesProperty() {
        return enableChatWithFiles;
    }

    public boolean getEnableChatWithFiles() {
        return enableChatWithFiles.get();
    }

    public void setEnableChatWithFiles(boolean enableChatWithFiles) {
        this.enableChatWithFiles.set(enableChatWithFiles);
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

    public StringProperty chatModelProperty() {
        return chatModel;
    }

    public String getChatModel() {
        return chatModel.get();
    }

    public void setChatModel(String chatModel) {
        this.chatModel.set(chatModel);
    }

    public StringProperty apiTokenProperty() {
        return apiToken;
    }

    public String getApiToken() {
        return apiToken.get();
    }

    public void setApiToken(String apiToken) {
        this.apiToken.set(apiToken);
    }

    public BooleanProperty customizeSettingsProperty() {
        return customizeSettings;
    }

    public boolean getCustomizeSettings() {
        return customizeSettings.get();
    }

    public void setCustomizeSettings(boolean customizeSettings) {
        this.customizeSettings.set(customizeSettings);
    }

    public ObjectProperty<EmbeddingModel> embeddingModelProperty() {
        return embeddingModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel.get();
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel.set(embeddingModel);
    }

    public StringProperty systemMessageProperty() {
        return systemMessage;
    }

    public String getSystemMessage() {
        return systemMessage.get();
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage.set(systemMessage);
    }

    public DoubleProperty temperatureProperty() {
        return temperature;
    }

    public double getTemperature() {
        return temperature.get();
    }

    public void setTemperature(double temperature) {
        this.temperature.set(temperature);
    }

    public IntegerProperty messageWindowSizeProperty() {
        return messageWindowSize;
    }

    public int getMessageWindowSize() {
        return messageWindowSize.get();
    }

    public void setMessageWindowSize(int messageWindowSize) {
        this.messageWindowSize.set(messageWindowSize);
    }

    public IntegerProperty documentSplitterChunkSizeProperty() {
        return documentSplitterChunkSize;
    }

    public int getDocumentSplitterChunkSize() {
        return documentSplitterChunkSize.get();
    }

    public void setDocumentSplitterChunkSize(int documentSplitterChunkSize) {
        this.documentSplitterChunkSize.set(documentSplitterChunkSize);
    }

    public IntegerProperty documentSplitterOverlapSizeProperty() {
        return documentSplitterOverlapSize;
    }

    public int getDocumentSplitterOverlapSize() {
        return documentSplitterOverlapSize.get();
    }

    public void setDocumentSplitterOverlapSize(int documentSplitterOverlapSize) {
        this.documentSplitterOverlapSize.set(documentSplitterOverlapSize);
    }

    public IntegerProperty ragMaxResultsCountProperty() {
        return ragMaxResultsCount;
    }

    public int getRagMaxResultsCount() {
        return ragMaxResultsCount.get();
    }

    public void setRagMaxResultsCount(int ragMaxResultsCount) {
        this.ragMaxResultsCount.set(ragMaxResultsCount);
    }

    public DoubleProperty ragMinScoreProperty() {
        return ragMinScore;
    }

    public double getRagMinScore() {
        return ragMinScore.get();
    }

    public void setRagMinScore(double ragMinScore) {
        this.ragMinScore.set(ragMinScore);
    }

    /**
     * Listen to changes of preferences that are related to embeddings generation.
     *
     * @param runnable The runnable that should be executed when the preferences change.
     */
    public void onEmbeddingsParametersChange(Runnable runnable) {
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
}
