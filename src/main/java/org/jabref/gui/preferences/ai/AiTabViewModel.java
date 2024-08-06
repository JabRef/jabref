package org.jabref.gui.preferences.ai;

import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

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
    private final BooleanProperty enableAi = new SimpleBooleanProperty();

    private final ListProperty<AiPreferences.AiProvider> aiProvidersList =
            new SimpleListProperty<>(FXCollections.observableArrayList(AiPreferences.AiProvider.values()));
    private final ObjectProperty<AiPreferences.AiProvider> selectedAiProvider = new SimpleObjectProperty<>();

    private final ListProperty<String> chatModelsList =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private final StringProperty currentChatModel = new SimpleStringProperty();

    private final StringProperty openAiChatModel = new SimpleStringProperty();
    private final StringProperty mistralAiChatModel = new SimpleStringProperty();
    private final StringProperty huggingFaceChatModel = new SimpleStringProperty();

    private final StringProperty currentApiToken = new SimpleStringProperty();

    private final StringProperty openAiApiToken = new SimpleStringProperty();
    private final StringProperty mistralAiApiToken = new SimpleStringProperty();
    private final StringProperty huggingFaceApiToken = new SimpleStringProperty();

    private final BooleanProperty customizeExpertSettings = new SimpleBooleanProperty();

    private final ListProperty<AiPreferences.EmbeddingModel> embeddingModelsList =
            new SimpleListProperty<>(FXCollections.observableArrayList(AiPreferences.EmbeddingModel.values()));
    private final ObjectProperty<AiPreferences.EmbeddingModel> selectedEmbeddingModel = new SimpleObjectProperty<>();

    private final StringProperty currentApiBaseUrl = new SimpleStringProperty();
    private final BooleanProperty disableApiBaseUrl = new SimpleBooleanProperty(true); // {@link HuggingFaceChatModel} doesn't support setting API base URL

    private final StringProperty openAiApiBaseUrl = new SimpleStringProperty();
    private final StringProperty mistralAiApiBaseUrl = new SimpleStringProperty();
    private final StringProperty huggingFaceApiBaseUrl = new SimpleStringProperty();

    private final StringProperty instruction = new SimpleStringProperty();
    private final DoubleProperty temperature = new SimpleDoubleProperty();
    private final IntegerProperty contextWindowSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterChunkSize = new SimpleIntegerProperty();
    private final IntegerProperty documentSplitterOverlapSize = new SimpleIntegerProperty();
    private final IntegerProperty ragMaxResultsCount = new SimpleIntegerProperty();
    private final DoubleProperty ragMinScore = new SimpleDoubleProperty();

    private final BooleanProperty disableBasicSettings = new SimpleBooleanProperty(true);
    private final BooleanProperty disableExpertSettings = new SimpleBooleanProperty(true);

    private final AiPreferences aiPreferences;

    private final Validator apiTokenValidator;
    private final Validator chatModelValidator;
    private final Validator apiBaseUrlValidator;
    private final Validator instructionValidator;
    private final Validator temperatureValidator;
    private final Validator contextWindowSizeValidator;
    private final Validator documentSplitterChunkSizeValidator;
    private final Validator documentSplitterOverlapSizeValidator;
    private final Validator ragMaxResultsCountValidator;
    private final Validator ragMinScoreValidator;

    public AiTabViewModel(PreferencesService preferencesService) {
        this.aiPreferences = preferencesService.getAiPreferences();

        this.enableAi.addListener((observable, oldValue, newValue) -> {
            disableBasicSettings.set(!newValue);
            disableExpertSettings.set(!newValue || !customizeExpertSettings.get());

            if (newValue) {
                openAiApiToken.set(preferencesService.getApiKeyForAiProvider(AiPreferences.AiProvider.OPEN_AI));
                mistralAiApiToken.set(preferencesService.getApiKeyForAiProvider(AiPreferences.AiProvider.MISTRAL_AI));
                huggingFaceApiToken.set(preferencesService.getApiKeyForAiProvider(AiPreferences.AiProvider.HUGGING_FACE));

                if (selectedAiProvider.get() != null) {
                    switch (selectedAiProvider.get()) {
                        case OPEN_AI -> {
                            currentApiToken.set(openAiApiToken.get());
                        }

                        case MISTRAL_AI -> {
                            currentApiToken.set(mistralAiApiToken.get());
                        }

                        case HUGGING_FACE -> {
                            currentApiToken.set(huggingFaceApiToken.get());
                        }
                    }
                }
            }
        });

        this.customizeExpertSettings.addListener((observableValue, oldValue, newValue) ->
                disableExpertSettings.set(!newValue || !enableAi.get())
        );

        this.selectedAiProvider.addListener((observable, oldValue, newValue) -> {
            List<String> models = AiDefaultPreferences.AVAILABLE_CHAT_MODELS.get(newValue);

            // When we setAll on Hugging Face, models are empty, and currentChatModel become null.
            // It becomes null beause currentChatModel is binded to combobox, and this combobox becomes empty.
            // For some reason, custom edited value in the combobox will be erased, so we need to store the old value.
            String oldChatModel = currentChatModel.get();
            chatModelsList.setAll(models);

            disableApiBaseUrl.set(newValue == AiPreferences.AiProvider.HUGGING_FACE);

            if (oldValue != null) {
                switch (oldValue) {
                    case OPEN_AI -> {
                        openAiChatModel.set(oldChatModel);
                        openAiApiToken.set(currentApiToken.get());
                        openAiApiBaseUrl.set(currentApiBaseUrl.get());
                    }

                    case MISTRAL_AI -> {
                        mistralAiChatModel.set(oldChatModel);
                        mistralAiApiToken.set(currentApiToken.get());
                        mistralAiApiBaseUrl.set(currentApiBaseUrl.get()); }

                    case HUGGING_FACE -> {
                        huggingFaceChatModel.set(oldChatModel);
                        huggingFaceApiToken.set(currentApiToken.get());
                        huggingFaceApiBaseUrl.set(currentApiBaseUrl.get());
                    }
                }
            }

            switch (newValue) {
                case OPEN_AI -> {
                    currentChatModel.set(openAiChatModel.get());
                    currentApiToken.set(openAiApiToken.get());
                    currentApiBaseUrl.set(openAiApiBaseUrl.get());
                }

                case MISTRAL_AI -> {
                    currentChatModel.set(mistralAiChatModel.get());
                    currentApiToken.set(mistralAiApiToken.get());
                    currentApiBaseUrl.set(mistralAiApiBaseUrl.get());
                }

                case HUGGING_FACE -> {
                    currentChatModel.set(huggingFaceChatModel.get());
                    currentApiToken.set(huggingFaceApiToken.get());
                    currentApiBaseUrl.set(huggingFaceApiBaseUrl.get());
                }
            }
        });

        this.currentChatModel.addListener((observable, oldValue, newValue) -> {
            switch (selectedAiProvider.get()) {
                case OPEN_AI -> openAiChatModel.set(newValue);
                case MISTRAL_AI -> mistralAiChatModel.set(newValue);
                case HUGGING_FACE -> huggingFaceChatModel.set(newValue);
            }

            Map<String, Integer> modelContextWindows = AiDefaultPreferences.CONTEXT_WINDOW_SIZES.get(selectedAiProvider.get());

            if (modelContextWindows == null) {
                contextWindowSize.set(AiDefaultPreferences.CONTEXT_WINDOW_SIZE);
                return;
            }

            contextWindowSize.set(modelContextWindows.getOrDefault(newValue, AiDefaultPreferences.CONTEXT_WINDOW_SIZE));
        });

        this.currentApiToken.addListener((observable, oldValue, newValue) -> {
            switch (selectedAiProvider.get()) {
                case OPEN_AI -> openAiApiToken.set(newValue);
                case MISTRAL_AI -> mistralAiApiToken.set(newValue);
                case HUGGING_FACE -> huggingFaceApiToken.set(newValue);
            }
        });

        this.currentApiBaseUrl.addListener((observable, oldValue, newValue) -> {
            switch (selectedAiProvider.get()) {
                case OPEN_AI -> openAiApiBaseUrl.set(newValue);
                case MISTRAL_AI -> mistralAiApiBaseUrl.set(newValue);
                case HUGGING_FACE -> huggingFaceApiBaseUrl.set(newValue);
            }
        });

        this.apiTokenValidator = new FunctionBasedValidator<>(
                currentApiToken,
                token -> !StringUtil.isBlank(token),
                ValidationMessage.error(Localization.lang("An OpenAI token has to be provided")));

        this.chatModelValidator = new FunctionBasedValidator<>(
                currentChatModel,
                chatModel -> !StringUtil.isBlank(chatModel),
                ValidationMessage.error(Localization.lang("Chat model has to be provided")));

        this.apiBaseUrlValidator = new FunctionBasedValidator<>(
                currentApiBaseUrl,
                token -> !StringUtil.isBlank(token),
                ValidationMessage.error(Localization.lang("API base URL has to be provided")));

        this.instructionValidator = new FunctionBasedValidator<>(
                instruction,
                message -> !StringUtil.isBlank(message),
                ValidationMessage.error(Localization.lang("The instruction has to be provided")));

        // Source: https://platform.openai.com/docs/api-reference/chat/create#chat-create-temperature
        this.temperatureValidator = new FunctionBasedValidator<>(
                temperature,
                temp -> temp.doubleValue() >= 0 && temp.doubleValue() <= 2,
                ValidationMessage.error(Localization.lang("Temperature must be between 0 and 2")));

        this.contextWindowSizeValidator = new FunctionBasedValidator<>(
                contextWindowSize,
                size -> size.intValue() > 0,
                ValidationMessage.error(Localization.lang("Context window size must be greater than 0")));

        this.documentSplitterChunkSizeValidator = new FunctionBasedValidator<>(
                documentSplitterChunkSize,
                size -> size.intValue() > 0,
                ValidationMessage.error(Localization.lang("Document splitter chunk size must be greater than 0")));

        this.documentSplitterOverlapSizeValidator = new FunctionBasedValidator<>(
                documentSplitterOverlapSize,
                size -> size.intValue() > 0 && size.intValue() < documentSplitterChunkSize.get(),
                ValidationMessage.error(Localization.lang("Document splitter overlap size must be greater than 0 and less than chunk size")));

        this.ragMaxResultsCountValidator = new FunctionBasedValidator<>(
                ragMaxResultsCount,
                count -> count.intValue() > 0,
                ValidationMessage.error(Localization.lang("RAG max results count must be greater than 0")));

        this.ragMinScoreValidator = new FunctionBasedValidator<>(
                ragMinScore,
                score -> score.doubleValue() > 0 && score.doubleValue() < 1,
                ValidationMessage.error(Localization.lang("RAG min score must be greater than 0 and less than 1")));
    }

    @Override
    public void setValues() {
        enableAi.setValue(aiPreferences.getEnableAi());

        openAiChatModel.setValue(aiPreferences.getOpenAiChatModel());
        mistralAiChatModel.setValue(aiPreferences.getMistralAiChatModel());
        huggingFaceChatModel.setValue(aiPreferences.getHuggingFaceChatModel());

        openAiApiToken.setValue(aiPreferences.getOpenAiApiToken());
        mistralAiApiToken.setValue(aiPreferences.getMistralAiApiToken());
        huggingFaceApiToken.setValue(aiPreferences.getHuggingFaceApiToken());

        customizeExpertSettings.setValue(aiPreferences.getCustomizeExpertSettings());

        selectedEmbeddingModel.setValue(aiPreferences.getEmbeddingModel());

        openAiApiBaseUrl.setValue(aiPreferences.getOpenAiApiBaseUrl());
        mistralAiApiBaseUrl.setValue(aiPreferences.getMistralAiApiBaseUrl());
        huggingFaceApiBaseUrl.setValue(aiPreferences.getHuggingFaceApiBaseUrl());

        instruction.setValue(aiPreferences.getInstruction());
        temperature.setValue(aiPreferences.getTemperature());
        contextWindowSize.setValue(aiPreferences.getContextWindowSize());
        documentSplitterChunkSize.setValue(aiPreferences.getDocumentSplitterChunkSize());
        documentSplitterOverlapSize.setValue(aiPreferences.getDocumentSplitterOverlapSize());
        ragMaxResultsCount.setValue(aiPreferences.getRagMaxResultsCount());
        ragMinScore.setValue(aiPreferences.getRagMinScore());

        selectedAiProvider.setValue(aiPreferences.getAiProvider());
    }

    @Override
    public void storeSettings() {
        aiPreferences.setEnableAi(enableAi.get());

        aiPreferences.setAiProvider(selectedAiProvider.get());

        aiPreferences.setOpenAiChatModel(openAiChatModel.get());
        aiPreferences.setMistralAiChatModel(mistralAiChatModel.get());
        aiPreferences.setHuggingFaceChatModel(huggingFaceChatModel.get());

        aiPreferences.setOpenAiApiToken(openAiApiToken.get());
        aiPreferences.setMistralAiApiToken(mistralAiApiToken.get());
        aiPreferences.setHuggingFaceApiToken(huggingFaceApiToken.get());

        aiPreferences.setCustomizeExpertSettings(customizeExpertSettings.get());

        aiPreferences.setEmbeddingModel(selectedEmbeddingModel.get());

        aiPreferences.setOpenAiApiBaseUrl(openAiApiBaseUrl.get());
        aiPreferences.setMistralAiApiBaseUrl(mistralAiApiBaseUrl.get());
        aiPreferences.setHuggingFaceApiBaseUrl(huggingFaceApiBaseUrl.get());

        aiPreferences.setInstruction(instruction.get());
        aiPreferences.setTemperature(temperature.get());
        aiPreferences.setContextWindowSize(contextWindowSize.get());
        aiPreferences.setDocumentSplitterChunkSize(documentSplitterChunkSize.get());
        aiPreferences.setDocumentSplitterOverlapSize(documentSplitterOverlapSize.get());
        aiPreferences.setRagMaxResultsCount(ragMaxResultsCount.get());
        aiPreferences.setRagMinScore(ragMinScore.get());
    }

    public void resetExpertSettings() {
        String resetApiBaseUrl = AiDefaultPreferences.PROVIDERS_API_URLS.get(selectedAiProvider.get());
        currentApiBaseUrl.set(resetApiBaseUrl);

        instruction.set(AiDefaultPreferences.SYSTEM_MESSAGE);

        int resetContextWindowSize = AiDefaultPreferences.CONTEXT_WINDOW_SIZES.getOrDefault(selectedAiProvider.get(), Map.of()).getOrDefault(currentChatModel.get(), 0);
        contextWindowSize.set(resetContextWindowSize);

        temperature.set(AiDefaultPreferences.TEMPERATURE);
        documentSplitterChunkSize.set(AiDefaultPreferences.DOCUMENT_SPLITTER_CHUNK_SIZE);
        documentSplitterOverlapSize.set(AiDefaultPreferences.DOCUMENT_SPLITTER_OVERLAP);
        ragMaxResultsCount.set(AiDefaultPreferences.RAG_MAX_RESULTS_COUNT);
        ragMinScore.set(AiDefaultPreferences.RAG_MIN_SCORE);
    }

    @Override
    public boolean validateSettings() {
        if (enableAi.get()) {
            if (customizeExpertSettings.get()) {
                return validateBasicSettings() && validateExpertSettings();
            } else {
                return validateBasicSettings();
            }
        }

        return true;
    }

    public boolean validateBasicSettings() {
        List<Validator> validators = List.of(
                chatModelValidator,
                apiTokenValidator
        );

        return validators.stream().map(Validator::getValidationStatus).allMatch(ValidationStatus::isValid);
    }

    public boolean validateExpertSettings() {
        List<Validator> validators = List.of(
                apiBaseUrlValidator,
                instructionValidator,
                temperatureValidator,
                contextWindowSizeValidator,
                documentSplitterChunkSizeValidator,
                documentSplitterOverlapSizeValidator,
                ragMaxResultsCountValidator,
                ragMinScoreValidator
        );

        return validators.stream().map(Validator::getValidationStatus).allMatch(ValidationStatus::isValid);
    }

    public BooleanProperty enableAi() {
        return enableAi;
    }

    public boolean getEnableAi() {
        return enableAi.get();
    }

    public ReadOnlyListProperty<AiPreferences.AiProvider> aiProvidersProperty() {
        return aiProvidersList;
    }

    public ObjectProperty<AiPreferences.AiProvider> selectedAiProviderProperty() {
        return selectedAiProvider;
    }

    public ReadOnlyListProperty<String> chatModelsProperty() {
        return chatModelsList;
    }

    public StringProperty selectedChatModelProperty() {
        return currentChatModel;
    }

    public StringProperty apiTokenProperty() {
        return currentApiToken;
    }

    public BooleanProperty customizeExpertSettingsProperty() {
        return customizeExpertSettings;
    }

    public ReadOnlyListProperty<AiPreferences.EmbeddingModel> embeddingModelsProperty() {
        return embeddingModelsList;
    }

    public ObjectProperty<AiPreferences.EmbeddingModel> selectedEmbeddingModelProperty() {
        return selectedEmbeddingModel;
    }

    public StringProperty apiBaseUrlProperty() {
        return currentApiBaseUrl;
    }

    public BooleanProperty disableApiBaseUrlProperty() {
        return disableApiBaseUrl;
    }

    public StringProperty instructionProperty() {
        return instruction;
    }

    public DoubleProperty temperatureProperty() {
        return temperature;
    }

    public IntegerProperty contextWindowSizeProperty() {
        return contextWindowSize;
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

    public BooleanProperty disableBasicSettingsProperty() {
        return disableBasicSettings;
    }

    public BooleanProperty disableExpertSettingsProperty() {
        return disableExpertSettings;
    }

    public ValidationStatus getApiTokenValidationStatus() {
        return apiTokenValidator.getValidationStatus();
    }

    public ValidationStatus getChatModelValidationStatus() {
        return chatModelValidator.getValidationStatus();
    }

    public ValidationStatus getApiBaseUrlValidationStatus() {
        return apiBaseUrlValidator.getValidationStatus();
    }

    public ValidationStatus getSystemMessageValidationStatus() {
        return instructionValidator.getValidationStatus();
    }

    public ValidationStatus getTemperatureValidationStatus() {
        return temperatureValidator.getValidationStatus();
    }

    public ValidationStatus getMessageWindowSizeValidationStatus() {
        return contextWindowSizeValidator.getValidationStatus();
    }

    public ValidationStatus getDocumentSplitterChunkSizeValidationStatus() {
        return documentSplitterChunkSizeValidator.getValidationStatus();
    }

    public ValidationStatus getDocumentSplitterOverlapSizeValidationStatus() {
        return documentSplitterOverlapSizeValidator.getValidationStatus();
    }

    public ValidationStatus getRagMaxResultsCountValidationStatus() {
        return ragMaxResultsCountValidator.getValidationStatus();
    }

    public ValidationStatus getRagMinScoreValidationStatus() {
        return ragMinScoreValidator.getValidationStatus();
    }
}
