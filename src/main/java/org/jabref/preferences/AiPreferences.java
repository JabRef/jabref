package org.jabref.preferences;

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
    public enum ChatModel {
        GPT_3_5_TURBO("gpt-3.5-turbo"),
        GPT_4("gpt-4"),
        GPT_4_TURBO("gpt-4-turbo"),
        GPT_4O("gpt-4o");

        private final String name;

        ChatModel(String name) {
            this.name = name;
        }

        public static ChatModel fromString(String text) {
            for (ChatModel b : ChatModel.values()) {
                if (b.name.equalsIgnoreCase(text)) {
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

    public enum EmbeddingModel {
        ALL_MINLM_l6_V2,
        ALL_MINLM_l6_V2_Q,
    }

    private final BooleanProperty enableChatWithFiles;
    private final StringProperty openAiToken;

    private final BooleanProperty customizeSettings;

    private final ObjectProperty<ChatModel> chatModel;
    private final ObjectProperty<EmbeddingModel> embeddingModel;

    private final StringProperty systemMessage;
    private final DoubleProperty temperature;
    private final IntegerProperty messageWindowSize;
    private final IntegerProperty documentSplitterChunkSize;
    private final IntegerProperty documentSplitterOverlapSize;
    private final IntegerProperty ragMaxResultsCount;
    private final DoubleProperty ragMinScore;

    public AiPreferences(boolean enableChatWithFiles, String openAiToken, ChatModel chatModel, EmbeddingModel embeddingModel, boolean customizeSettings, String systemMessage, double temperature, int messageWindowSize, int documentSplitterChunkSize, int documentSplitterOverlapSize, int ragMaxResultsCount, double ragMinScore) {
        this.enableChatWithFiles = new SimpleBooleanProperty(enableChatWithFiles);
        this.openAiToken = new SimpleStringProperty(openAiToken);

        this.customizeSettings = new SimpleBooleanProperty(customizeSettings);

        this.chatModel = new SimpleObjectProperty<>(chatModel);
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

    public StringProperty openAiTokenProperty() {
        return openAiToken;
    }

    public String getOpenAiToken() {
        return openAiToken.get();
    }

    public void setOpenAiToken(String openAiToken) {
        this.openAiToken.set(openAiToken);
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

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }

    public ChatModel getChatModel() {
        return chatModel.get();
    }

    public void setChatModel(ChatModel chatModel) {
        this.chatModel.set(chatModel);
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
}
