package org.jabref.preferences;

import java.util.List;
import java.util.EnumMap;
import java.util.List;
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

import org.jabref.gui.entryeditor.aichattab.AiChatTab;

public class AiPreferences {
    public enum AiProvider {
        OPEN_AI("OpenAI"),
        MISTRAL_AI("Mistral AI"),
        HUGGING_FACE("Hugging Face");

        private final String label;

        AiProvider(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String toString() {
            return label;
        }
    }

    public static final Map<AiProvider, List<String>> CHAT_MODELS = new EnumMap<>(AiProvider.class);

    static {
        CHAT_MODELS.put(AiProvider.OPEN_AI, List.of("gpt-3.5-turbo", "gpt-4", "gpt-4-turbo", "gpt-4o"));
        CHAT_MODELS.put(AiProvider.MISTRAL_AI, List.of("open-mistral-7b", "open-mixtral-8x7b", "open-mixtral-8x22b", "mistral-small-latest", "mistral-medium-latest", "mistral-large-latest"));
        CHAT_MODELS.put(AiProvider.HUGGING_FACE, List.of());
    }

    public enum EmbeddingModel {
        ALL_MINILM_L6_V2("all-MiniLM-L6-v2");

        private final String label;

        EmbeddingModel(String label) {
            this.label = label;
        }

        public String getLabel() {
             return label;
        }

        public String toString() {
            return label;
        }
    }

    private final BooleanProperty enableChatWithFiles;

    private final ObjectProperty<AiProvider> aiProvider;
    private final StringProperty chatModel;
    private final StringProperty apiToken;

    private final BooleanProperty customizeSettings;

    private final ObjectProperty<EmbeddingModel> embeddingModel;
    private final StringProperty apiBaseUrl;
    private final StringProperty instruction;
    private final DoubleProperty temperature;
    private final IntegerProperty contextWindowSize;
    private final IntegerProperty documentSplitterChunkSize;
    private final IntegerProperty documentSplitterOverlapSize;
    private final IntegerProperty ragMaxResultsCount;
    private final DoubleProperty ragMinScore;

    public AiPreferences(boolean enableChatWithFiles,
                         AiProvider aiProvider,
                         String chatModel,
                         String apiToken,
                         boolean customizeSettings,
                         EmbeddingModel embeddingModel,
                         String apiBaseUrl,
                         String instruction,
                         double temperature,
                         int contextWindowSize,
                         int documentSplitterChunkSize,
                         int documentSplitterOverlapSize,
                         int ragMaxResultsCount,
                         double ragMinScore
    ) {
        this.enableChatWithFiles = new SimpleBooleanProperty(enableChatWithFiles);

        this.aiProvider = new SimpleObjectProperty<>(aiProvider);
        this.chatModel = new SimpleStringProperty(chatModel);
        this.apiToken = new SimpleStringProperty(apiToken);

        this.customizeSettings = new SimpleBooleanProperty(customizeSettings);

        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
        this.apiBaseUrl = new SimpleStringProperty(apiBaseUrl);
        this.instruction = new SimpleStringProperty(instruction);
        this.temperature = new SimpleDoubleProperty(temperature);
        this.contextWindowSize = new SimpleIntegerProperty(contextWindowSize);
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

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl.set(apiBaseUrl);
    }

    public StringProperty apiBaseUrlProperty() {
        return apiBaseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl.get();
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel.set(embeddingModel);
    }

    public StringProperty instructionProperty() {
        return instruction;
    }

    public String getInstruction() {
        return instruction.get();
    }

    public void setInstruction(String instruction) {
        this.instruction.set(instruction);
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

    public IntegerProperty contextWindowSizeProperty() {
        return contextWindowSize;
    }

    public int getContextWindowSize() {
        return contextWindowSize.get();
    }

    public void setContextWindowSize(int contextWindowSize) {
        this.contextWindowSize.set(contextWindowSize);
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

    /**
     * Listen to all changes of preferences related to AI.
     * This method is used in {@link AiChatTab} to update itself when preferences change.
     * JabRef would close the entry editor, but the last selected entry editor is not refreshed.
     *
     * @param runnable The runnable that should be executed when the preferences change.
     */
    public void onAnyParametersChange(Runnable runnable) {
        enableChatWithFiles.addListener((observableValue, oldValue, newValue) -> {
            if (oldValue != newValue) {
                runnable.run();
            }
        });

        apiToken.addListener((observableValue, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                runnable.run();
            }
        });

        customizeSettings.addListener((observableValue, oldValue, newValue) -> {
            if (oldValue != newValue) {
                runnable.run();
            }
        });

        chatModel.addListener((observableValue, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                runnable.run();
            }
        });

        embeddingModel.addListener((observableValue, oldValue, newValue) -> {
            if (oldValue != newValue) {
                runnable.run();
            }
        });

        instruction.addListener((observableValue, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                runnable.run();
            }
        });

        temperature.addListener((observableValue, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                runnable.run();
            }
        });

        contextWindowSize.addListener((observableValue, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                runnable.run();
            }
        });

        onEmbeddingsParametersChange(runnable);

        ragMaxResultsCount.addListener((observableValue, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                runnable.run();
            }
        });

        ragMinScore.addListener((observableValue, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                runnable.run();
            }
        });
    }
}
