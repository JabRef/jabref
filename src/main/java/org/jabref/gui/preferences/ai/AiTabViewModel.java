package org.jabref.gui.preferences.ai;

import java.util.Arrays;

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

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.ai.AiDefaultPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class AiTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty useAi = new SimpleBooleanProperty();

    private final StringProperty openAiToken = new SimpleStringProperty();

    private final BooleanProperty customizeSettings = new SimpleBooleanProperty();

    private final ObjectProperty<AiPreferences.ChatModel> aiModel = new SimpleObjectProperty<>();
    private final ObjectProperty<AiPreferences.EmbeddingModel> embeddingModel = new SimpleObjectProperty<>();

    private final StringProperty systemMessage = new SimpleStringProperty();
    private final DoubleProperty temperature = new SimpleDoubleProperty();
    private final IntegerProperty messageWindowSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterChunkSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterOverlapSize = new SimpleIntegerProperty();
    private final IntegerProperty ragMaxResultsCount = new SimpleIntegerProperty();
    private final DoubleProperty ragMinScore = new SimpleDoubleProperty();

    private final AiPreferences aiPreferences;

    private final Validator openAiTokenValidator;
    private final Validator systemMessageValidator;
    private final Validator temperatureValidator;
    private final Validator messageWindowSizeValidator;
    private final Validator documentSplitterChunkSizeValidator;
    private final Validator documentSplitterOverlapSizeValidator;
    private final Validator ragMaxResultsCountValidator;
    private final Validator ragMinScoreValidator;

    public AiTabViewModel(PreferencesService preferencesService) {
        this.aiPreferences = preferencesService.getAiPreferences();

        this.openAiTokenValidator = new FunctionBasedValidator<>(
                openAiToken,
                (token) -> !StringUtil.isBlank(token),
                ValidationMessage.error(Localization.lang("The OpenAI token cannot be empty")));

        this.systemMessageValidator = new FunctionBasedValidator<>(
                systemMessage,
                (message) -> !StringUtil.isBlank(message),
                ValidationMessage.error(Localization.lang("The system message cannot be empty")));

        this.temperatureValidator = new FunctionBasedValidator<>(
                temperature,
                (temp) -> (double)temp >= 0 && (double)temp <= 2,
                ValidationMessage.error(Localization.lang("Temperature must be between 0 and 2")));

        this.messageWindowSizeValidator = new FunctionBasedValidator<>(
                messageWindowSize,
                (size) -> (int)size > 0,
                ValidationMessage.error(Localization.lang("Message window size must be greater than 0")));

        this.documentSplitterChunkSizeValidator = new FunctionBasedValidator<>(
                documentSplitterChunkSize,
                (size) -> (int)size > 0,
                ValidationMessage.error(Localization.lang("Document splitter chunk size must be greater than 0")));

        this.documentSplitterOverlapSizeValidator = new FunctionBasedValidator<>(
                documentSplitterOverlapSize,
                (size) -> (int)size > 0 && (int)size < documentSplitterChunkSize.get(),
                ValidationMessage.error(Localization.lang("Document splitter overlap size must be greater than 0 and less than chunk size")));

        this.ragMaxResultsCountValidator = new FunctionBasedValidator<>(
                ragMaxResultsCount,
                (count) -> (int)count > 0,
                ValidationMessage.error(Localization.lang("RAG max results count must be greater than 0")));

        this.ragMinScoreValidator = new FunctionBasedValidator<>(
                ragMinScore,
                (score) -> (double)score > 0 && (double)score < 1,
                ValidationMessage.error(Localization.lang("RAG min score must be greater than 0 and less than 1")));
    }

    @Override
    public void setValues() {
        useAi.setValue(aiPreferences.getEnableChatWithFiles());
        openAiToken.setValue(aiPreferences.getOpenAiToken());

        customizeSettings.setValue(aiPreferences.getCustomizeSettings());

        aiModel.setValue(aiPreferences.getChatModel());
        embeddingModel.setValue(aiPreferences.getEmbeddingModel());

        systemMessage.setValue(aiPreferences.getSystemMessage());
        temperature.setValue(aiPreferences.getTemperature());
        messageWindowSize.setValue(aiPreferences.getMessageWindowSize());
        documentSplitterChunkSize.setValue(aiPreferences.getDocumentSplitterChunkSize());
        documentSplitterOverlapSize.setValue(aiPreferences.getDocumentSplitterOverlapSize());
        ragMaxResultsCount.setValue(aiPreferences.getRagMaxResultsCount());
        ragMinScore.setValue(aiPreferences.getRagMinScore());
    }

    @Override
    public void storeSettings() {
        aiPreferences.setEnableChatWithFiles(useAi.get());
        aiPreferences.setOpenAiToken(openAiToken.get());

        aiPreferences.setCustomizeSettings(customizeSettings.get());

        if (customizeSettings.get()) {
            aiPreferences.setChatModel(aiModel.get());
            aiPreferences.setEmbeddingModel(embeddingModel.get());

            aiPreferences.setSystemMessage(systemMessage.get());
            aiPreferences.setTemperature(temperature.get());
            aiPreferences.setMessageWindowSize(messageWindowSize.get());
            aiPreferences.setDocumentSplitterChunkSize(documentSplitterChunkSize.get());
            aiPreferences.setDocumentSplitterOverlapSize(documentSplitterOverlapSize.get());
            aiPreferences.setRagMaxResultsCount(ragMaxResultsCount.get());
            aiPreferences.setRagMinScore(ragMinScore.get());
        } else {
            resetExpertSettings();
        }
    }

    public void resetExpertSettings() {
        // TODO: How to access default settings?
        aiPreferences.setSystemMessage(AiDefaultPreferences.SYSTEM_MESSAGE);
        systemMessage.set(AiDefaultPreferences.SYSTEM_MESSAGE);

        aiPreferences.setMessageWindowSize(AiDefaultPreferences.MESSAGE_WINDOW_SIZE);
        messageWindowSize.set(AiDefaultPreferences.MESSAGE_WINDOW_SIZE);

        aiPreferences.setDocumentSplitterChunkSize(AiDefaultPreferences.DOCUMENT_SPLITTER_CHUNK_SIZE);
        documentSplitterChunkSize.set(AiDefaultPreferences.DOCUMENT_SPLITTER_CHUNK_SIZE);

        aiPreferences.setDocumentSplitterOverlapSize(AiDefaultPreferences.DOCUMENT_SPLITTER_OVERLAP);
        documentSplitterOverlapSize.set(AiDefaultPreferences.DOCUMENT_SPLITTER_OVERLAP);

        aiPreferences.setRagMaxResultsCount(AiDefaultPreferences.RAG_MAX_RESULTS_COUNT);
        ragMaxResultsCount.set(AiDefaultPreferences.RAG_MAX_RESULTS_COUNT);

        aiPreferences.setRagMinScore(AiDefaultPreferences.RAG_MIN_SCORE);
        ragMinScore.set(AiDefaultPreferences.RAG_MIN_SCORE);
    }

    @Override
    public boolean validateSettings() {
        Validator[] validators = {
                openAiTokenValidator,
                systemMessageValidator,
                temperatureValidator,
                messageWindowSizeValidator,
                documentSplitterChunkSizeValidator,
                documentSplitterOverlapSizeValidator,
                ragMaxResultsCountValidator,
                ragMinScoreValidator
        };

        return Arrays.stream(validators).map(Validator::getValidationStatus).allMatch(ValidationStatus::isValid);
    }

    public StringProperty openAiTokenProperty() {
        return openAiToken;
    }

    public BooleanProperty useAiProperty() {
        return useAi;
    }

    public BooleanProperty customizeSettingsProperty() {
        return customizeSettings;
    }

    public ObjectProperty<AiPreferences.ChatModel> aiModelProperty() {
        return aiModel;
    }

    public ObjectProperty<AiPreferences.EmbeddingModel> embeddingModelProperty() {
        return embeddingModel;
    }

    public StringProperty systemMessageProperty() {
        return systemMessage;
    }

    public DoubleProperty temperatureProperty() {
        return temperature;
    }

    public IntegerProperty messageWindowSizeProperty() {
        return messageWindowSize;
    }

    public IntegerProperty documentSplitterChunkSizeProperty() {
        return documentSplitterChunkSize;
    }

    public IntegerProperty documentSplitterOverlapSizeProperty() {
        return documentSplitterOverlapSize;
    }

    public IntegerProperty ragMaxResultsCountProperty() {
        return ragMaxResultsCount;
    }

    public DoubleProperty ragMinScoreProperty() {
        return ragMinScore;
    }

    public ValidationStatus getOpenAiTokenValidatorStatus() {
        return openAiTokenValidator.getValidationStatus();
    }

    public ValidationStatus getSystemMessageValidatorStatus() {
        return systemMessageValidator.getValidationStatus();
    }

    public ValidationStatus getTemperatureValidatorStatus() {
        return temperatureValidator.getValidationStatus();
    }

    public ValidationStatus getMessageWindowSizeValidatorStatus() {
        return messageWindowSizeValidator.getValidationStatus();
    }

    public ValidationStatus getDocumentSplitterChunkSizeValidatorStatus() {
        return documentSplitterChunkSizeValidator.getValidationStatus();
    }

    public ValidationStatus getDocumentSplitterOverlapSizeValidatorStatus() {
        return documentSplitterOverlapSizeValidator.getValidationStatus();
    }

    public ValidationStatus getRagMaxResultsCountValidatorStatus() {
        return ragMaxResultsCountValidator.getValidationStatus();
    }

    public ValidationStatus getRagMinScoreValidatorStatus() {
        return ragMinScoreValidator.getValidationStatus();
    }
}
