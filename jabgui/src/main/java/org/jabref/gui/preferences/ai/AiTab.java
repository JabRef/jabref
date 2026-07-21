package org.jabref.gui.preferences.ai;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.gui.preferences.forms.PasswordFieldEditor;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;
import org.jabref.model.ai.llm.AiProvider;

import com.airhacks.afterburner.injection.Injector;
import com.dlsc.gemsfx.EnhancedPasswordField;
import com.dlsc.unitfx.IntegerInputField;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class AiTab extends AbstractFormTabView<AiTabViewModel> {

    private static final String HUGGING_FACE_CHAT_MODEL_PROMPT = "TinyLlama/TinyLlama_v1.1 (or any other model name)";

    /// Decorates the controls of the expert grid, which is assembled outside the builder.
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    private final BooleanBinding aiDisabled;

    private TabPane templatesTabPane;

    public AiTab() {
        this.viewModel = new AiTabViewModel(
                preferences,
                Injector.instantiateModelOrService(AiService.class).getModelService(),
                taskExecutor);
        this.aiDisabled = viewModel.enableAi().not();

        visualizer.setDecoration(new IconValidationDecorator());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("AI");
    }

    /// The AI master switch, consumed by the web search tab.
    public ReadOnlyBooleanProperty aiEnabledProperty() {
        return viewModel.enableAi();
    }

    private void buildView() {
        EnhancedPasswordField apiKey = PasswordFieldEditor.create(viewModel.apiKeyProperty())
                                                          .withRevealButton()
                                                          .withClearButton()
                                                          .build();

        getChildren().add(form()
                .title(Localization.lang("AI"))

                .sectionWithHelp(Localization.lang("General"), HelpFile.AI_GENERAL_SETTINGS, general -> general
                        // [impl->feat~ai.llms.providers~1]
                        .checkbox(Localization.lang("Enable AI functionality in JabRef"), viewModel.enableAi())
                        .info(Localization.lang("AI functionality in Jabref includes:"))
                        .info(Localization.lang("• Chatting with entries."))
                        .info(Localization.lang("• Summarizing entries."))
                        .info(Localization.lang("• Turn a citation into a BibTeX or BibLaTeX entry.")))

                .section(Localization.lang("Connection"), connection -> connection
                        .combo(Localization.lang("AI provider"),
                                viewModel.aiProvidersProperty(),
                                viewModel.selectedAiProviderProperty(),
                                AiNamingUtils::getDisplayName,
                                provider -> provider.disableWhen(viewModel.disableBasicSettingsProperty()))
                        .field(Localization.lang("Chat model"), buildChatModelCombo(),
                                chatModel -> chatModel.disableWhen(viewModel.disableBasicSettingsProperty())
                                                      .validate(viewModel.getChatModelValidationStatus()))
                        .field(Localization.lang("API key"), apiKey,
                                key -> key.disableWhen(viewModel.disableBasicSettingsProperty())
                                          .validate(viewModel.getApiTokenValidationStatus())))

                .sectionWithHelp(Localization.lang("Expert settings"), HelpFile.AI_EXPERT_SETTINGS, expertSettings -> expertSettings
                        .checkbox(Localization.lang("Customize expert settings"), viewModel.customizeExpertSettingsProperty(),
                                customize -> customize.disableWhen(viewModel.disableBasicSettingsProperty()))
                        .group(expert -> expert
                                        .stringField(Localization.lang("API base URL (used only for LLM)"), viewModel.apiBaseUrlProperty(),
                                                baseUrl -> baseUrl.disableWhen(viewModel.disableApiBaseUrlProperty())
                                                                  .validate(viewModel.getApiBaseUrlValidationStatus()))
                                        .searchableCombo(Localization.lang("Embedding model"),
                                                viewModel.embeddingModelsProperty(),
                                                viewModel.selectedEmbeddingModelProperty(),
                                                PredefinedEmbeddingModel::fullInfo,
                                                embedding -> embedding.validate(viewModel.getEmbeddingModelValidationStatus()))
                                        .info(Localization.lang("The size of the embedding model could be smaller than written in the list."))
                                        .custom(buildExpertGrid())
                                        .button(Localization.lang("Reset expert settings to default"), IconTheme.JabRefIcons.REFRESH, viewModel::resetExpertSettings),
                                // Disabling the group covers every expert control above; individual controls
                                // only add their own extra conditions on top.
                                expertGroup -> expertGroup.visibleWhen(viewModel.customizeExpertSettingsProperty())
                                                          .disableWhen(viewModel.disableExpertSettingsProperty())))

                // [impl->req~ai.expert-settings.templates~1]
                .sectionWithHelp(Localization.lang("Templates"), HelpFile.AI_TEMPLATES, templates -> templates
                        .custom(buildTemplatesRegion()))

                .section(Localization.lang("Miscellaneous"), miscellaneous -> miscellaneous
                        // [impl->req~ai.ingestion.automatic-trigger~1]
                        .checkbox(Localization.lang("Automatically generate embeddings for new entries"), viewModel.autoGenerateEmbeddings(),
                                embeddings -> embeddings.disableWhen(Bindings.or(aiDisabled, viewModel.disableAutoGenerateEmbeddings())))
                        // [impl->req~ai.summarization.entries.auto~1]
                        .checkbox(Localization.lang("Automatically generate summaries for new entries"), viewModel.autoGenerateSummaries(),
                                summaries -> summaries.disableWhen(Bindings.or(aiDisabled, viewModel.disableAutoGenerateSummaries())))
                        .checkbox(Localization.lang("Generate follow-up questions after AI response"), viewModel.generateFollowUpQuestionsProperty(),
                                followUp -> followUp.disableWhen(viewModel.disableBasicSettingsProperty()))
                        .field(Localization.lang("Number of follow-up questions"), buildFollowUpQuestionsCountSpinner())
                        // [impl->req~ai.response-engines.default~1]
                        .comboItems(Localization.lang("Default response engine"),
                                viewModel.responseEngineKindsProperty(),
                                viewModel.responseEngineProperty(),
                                AiNamingUtils::getDisplayName,
                                engine -> engine.disableWhen(viewModel.disableExpertSettingsProperty()))
                        // [impl->req~ai.summarization.algorithm.default~1]
                        .comboItems(Localization.lang("Default summarization algorithm"),
                                viewModel.summarizationAlgorithmsProperty(),
                                viewModel.summarizationAlgorithmProperty(),
                                AiNamingUtils::getDisplayName,
                                algorithm -> algorithm.disableWhen(viewModel.disableExpertSettingsProperty()))
                        .comboItems(Localization.lang("Default token estimation algorithm"),
                                viewModel.tokenEstimationAlgorithmsProperty(),
                                viewModel.tokenEstimationAlgorithmProperty(),
                                AiNamingUtils::getDisplayName,
                                estimator -> estimator.disableWhen(viewModel.disableExpertSettingsProperty())))
                .build());
    }

    /// Editable combo whose prompt switches to a model-name hint once Hugging Face is selected.
    private ComboBox<String> buildChatModelCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.itemsProperty().bind(viewModel.chatModelsProperty());
        combo.valueProperty().bindBidirectional(viewModel.selectedChatModelProperty());
        viewModel.selectedAiProviderProperty().addListener((_, _, newValue) -> {
            if (newValue == AiProvider.HUGGING_FACE) {
                combo.setPromptText(HUGGING_FACE_CHAT_MODEL_PROMPT);
            }
        });
        return combo;
    }

    private Spinner<Integer> buildFollowUpQuestionsCountSpinner() {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setEditable(true);
        spinner.setMaxWidth(100.0);
        spinner.setValueFactory(AiTabViewModel.followUpQuestionsCountValueFactory);
        spinner.getValueFactory().valueProperty().bindBidirectional(viewModel.followUpQuestionsCountProperty().asObject());
        spinner.disableProperty().bind(viewModel.generateFollowUpQuestionsProperty().not());
        return spinner;
    }

    /// The six numeric expert settings, laid out as two equal columns of label-above-field cells.
    // [impl->req~ai.expert-settings.chat-inference-global~1]
    // [impl->req~ai.expert-settings.rag-global~1]
    private Node buildExpertGrid() {
        IntegerInputField contextWindowSize = integerField(viewModel.contextWindowSizeProperty());
        TextField temperature = textField(viewModel.temperatureProperty());
        IntegerInputField ragMaxResultsCount = integerField(viewModel.ragMaxResultsCountProperty());
        TextField ragMinScore = textField(viewModel.ragMinScoreProperty());
        IntegerInputField documentSplitterChunkSize = integerField(viewModel.documentSplitterChunkSizeProperty());
        IntegerInputField documentSplitterOverlapSize = integerField(viewModel.documentSplitterOverlapSizeProperty());

        Platform.runLater(() -> {
            visualize(viewModel.getTemperatureTypeValidationStatus(), temperature);
            visualize(viewModel.getTemperatureRangeValidationStatus(), temperature);
            visualize(viewModel.getMessageWindowSizeValidationStatus(), contextWindowSize);
            visualize(viewModel.getDocumentSplitterChunkSizeValidationStatus(), documentSplitterChunkSize);
            visualize(viewModel.getDocumentSplitterOverlapSizeValidationStatus(), documentSplitterOverlapSize);
            visualize(viewModel.getRagMaxResultsCountValidationStatus(), ragMaxResultsCount);
            visualize(viewModel.getRagMinScoreTypeValidationStatus(), ragMinScore);
            visualize(viewModel.getRagMinScoreRangeValidationStatus(), ragMinScore);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10.0);
        grid.setVgap(10.0);
        ColumnConstraints half = new ColumnConstraints();
        half.setHgrow(Priority.ALWAYS);
        half.setPercentWidth(50.0);
        ColumnConstraints otherHalf = new ColumnConstraints();
        otherHalf.setHgrow(Priority.ALWAYS);
        otherHalf.setPercentWidth(50.0);
        grid.getColumnConstraints().addAll(half, otherHalf);

        grid.add(labelledCell(Localization.lang("Context window size"), contextWindowSize), 0, 0);
        grid.add(labelledCell(Localization.lang("Temperature"), temperature), 1, 0);
        grid.add(labelledCell(Localization.lang("RAG - maximum results count"), ragMaxResultsCount), 0, 1);
        grid.add(labelledCell(Localization.lang("RAG - minimum score"), ragMinScore), 1, 1);
        grid.add(labelledCell(Localization.lang("Document splitter - chunk size"), documentSplitterChunkSize), 0, 2);
        grid.add(labelledCell(Localization.lang("Document splitter - overlap size"), documentSplitterOverlapSize), 1, 2);
        return grid;
    }

    private void visualize(ValidationStatus status, javafx.scene.control.Control control) {
        visualizer.initVisualization(status, control);
    }

    private Node labelledCell(String text, Node control) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        return new VBox(10.0, label, control);
    }

    /// {@link IntegerInputField} holds a nullable `Integer`; mirror it onto the view model's
    /// primitive property in both directions, mapping `null` to zero.
    private IntegerInputField integerField(IntegerProperty value) {
        IntegerInputField field = new IntegerInputField();
        field.valueProperty().addListener((_, _, newValue) -> value.set(newValue == null ? 0 : newValue));
        value.addListener((_, _, newValue) -> field.valueProperty().set(newValue == null ? 0 : newValue.intValue()));
        return field;
    }

    private TextField textField(StringProperty value) {
        TextField field = new TextField();
        field.textProperty().bindBidirectional(value);
        return field;
    }

    /// One tab per prompt template, plus the two reset buttons underneath.
    private Node buildTemplatesRegion() {
        templatesTabPane = new TabPane();
        templatesTabPane.getTabs().addAll(
                // [impl->req~ai.chat.customize-system-prompt~1]
                templateTab(Localization.lang("System message for chatting"), viewModel.chattingSystemMessageTemplateProperty(), viewModel::resetChattingSystemMessageTemplate),
                // [impl->req~ai.response-engines.embeddings-search.prompt~1]
                // [impl->req~ai.response-engines.full-document.prompt~1]
                templateTab(Localization.lang("User message for chatting"), viewModel.chattingUserMessageTemplateProperty(), viewModel::resetChattingUserMessageTemplate),
                // [impl->req~ai.summarization.algorithms.chunked.system-prompt-chunk~1]
                templateTab(Localization.lang("System message for summarization of a chunk"), viewModel.summarizationChunkSystemMessageTemplateProperty(), viewModel::resetSummarizationChunkSystemMessageTemplate),
                // [impl->req~ai.summarization.algorithms.chunked.system-prompt-combine~1]
                templateTab(Localization.lang("System message for summarization of several chunks"), viewModel.summarizationCombineSystemMessageTemplateProperty(), viewModel::resetSummarizationCombineSystemMessageTemplate),
                // [impl->req~ai.summarization.algorithms.full.system-prompt~1]
                templateTab(Localization.lang("System message for 'full document' summarization"), viewModel.summarizationFullDocumentSystemMessageTemplateProperty(), viewModel::resetSummarizationFullDocumentSystemMessageTemplate),
                // [impl->req~ai.citation-parsing.system-prompt-config~1]
                templateTab(Localization.lang("System message for parsing raw citations"), viewModel.citationParsingSystemMessageTemplateProperty(), viewModel::resetCitationParsingSystemMessageTemplate),
                templateTab(Localization.lang("Markdown chat export template"), viewModel.markdownChatExportTemplateProperty(), viewModel::resetMarkdownChatExportTemplate),
                templateTab(Localization.lang("Template for follow-up questions"), viewModel.followUpQuestionsTemplateProperty(), viewModel::resetFollowUpQuestionsTemplate));

        Button resetCurrent = resetButton(Localization.lang("Reset current template"), this::resetCurrentTemplate);
        Button resetAll = resetButton(Localization.lang("Reset templates to default"), viewModel::resetTemplates);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(10.0, resetCurrent, spacer, resetAll);
        buttons.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10.0, templatesTabPane, buttons);
    }

    /// A template tab; its reset action is carried on the tab itself so that "reset current
    /// template" needs no chain of identity comparisons.
    ///
    /// The text area sits in a wrapper rather than being the content directly: {@link Tab} writes
    /// through to its content's `disable` property (on {@link Tab#setContent}, on being added to a
    /// {@link TabPane}, and whenever the tab itself is disabled), which throws against a bound
    /// value. The wrapper absorbs those writes; the text area keeps its own binding and the
    /// effective state is the union of both.
    private Tab templateTab(String title, StringProperty template, Runnable reset) {
        TextArea textArea = new TextArea();
        textArea.textProperty().bindBidirectional(template);
        textArea.disableProperty().bind(aiDisabled);

        Tab tab = new Tab(title, new StackPane(textArea));
        tab.setClosable(false);
        tab.setUserData(reset);
        return tab;
    }

    private void resetCurrentTemplate() {
        Tab selected = templatesTabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getUserData() instanceof Runnable reset) {
            reset.run();
        }
    }

    private Button resetButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setGraphic(IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        button.setOnAction(_ -> action.run());
        button.disableProperty().bind(aiDisabled);
        return button;
    }
}
