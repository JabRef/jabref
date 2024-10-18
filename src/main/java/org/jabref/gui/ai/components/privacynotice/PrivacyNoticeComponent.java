package org.jabref.gui.ai.components.privacynotice;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.model.ai.AiProvider;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivacyNoticeComponent extends ScrollPane {
    private final Logger LOGGER = LoggerFactory.getLogger(PrivacyNoticeComponent.class);

    @FXML private TextFlow openAiPrivacyTextFlow;
    @FXML private TextFlow mistralAiPrivacyTextFlow;
    @FXML private TextFlow geminiPrivacyTextFlow;
    @FXML private TextFlow huggingFacePrivacyTextFlow;
    @FXML private Text embeddingModelText;

    private final AiPreferences aiPreferences;
    private final Runnable onIAgreeButtonClickCallback;
    private final DialogService dialogService;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;

    public PrivacyNoticeComponent(AiPreferences aiPreferences, Runnable onIAgreeButtonClickCallback, ExternalApplicationsPreferences externalApplicationsPreferences, DialogService dialogService) {
        this.aiPreferences = aiPreferences;
        this.onIAgreeButtonClickCallback = onIAgreeButtonClickCallback;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.dialogService = dialogService;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        initPrivacyHyperlink(openAiPrivacyTextFlow, AiProvider.OPEN_AI);
        initPrivacyHyperlink(mistralAiPrivacyTextFlow, AiProvider.MISTRAL_AI);
        initPrivacyHyperlink(geminiPrivacyTextFlow, AiProvider.GEMINI);
        initPrivacyHyperlink(huggingFacePrivacyTextFlow, AiProvider.HUGGING_FACE);

        String newEmbeddingModelText = embeddingModelText.getText().replaceAll("%0", aiPreferences.getEmbeddingModel().sizeInfo());
        embeddingModelText.setText(newEmbeddingModelText);

        // Because of the https://bugs.openjdk.org/browse/JDK-8090400 bug, the text in the privacy policy cannot be
        // fully wrapped.

        embeddingModelText.wrappingWidthProperty().bind(this.widthProperty());
    }

    private void initPrivacyHyperlink(TextFlow textFlow, AiProvider aiProvider) {
        if (textFlow.getChildren().isEmpty() || !(textFlow.getChildren().getFirst() instanceof Text text)) {
            return;
        }

        String replacedText = text.getText().replaceAll("%0", aiProvider.getLabel()).replace("%1", "");

        replacedText = replacedText.endsWith(".") ? replacedText.substring(0, replacedText.length() - 1) : replacedText;

        text.setText(replacedText);
        text.wrappingWidthProperty().bind(this.widthProperty());

        Hyperlink hyperlink = new Hyperlink(aiProvider.getApiUrl());
        hyperlink.setWrapText(true);
        hyperlink.setFont(text.getFont());
        hyperlink.setOnAction(event -> {
            openBrowser(aiProvider.getApiUrl());
        });

        textFlow.getChildren().add(hyperlink);

        Text dot = new Text(".");
        dot.setFont(text.getFont());
        dot.wrappingWidthProperty().bind(this.widthProperty());

        textFlow.getChildren().add(dot);
    }

    @FXML
    private void onIAgreeButtonClick() {
        aiPreferences.setEnableAi(true);
        onIAgreeButtonClickCallback.run();
    }

    @FXML
    private void onDjlPrivacyPolicyClick() {
        openBrowser("https://github.com/deepjavalibrary/djl/discussions/3370#discussioncomment-10233632");
    }

    private void openBrowser(String link) {
        try {
            NativeDesktop.openBrowser(link, externalApplicationsPreferences);
        } catch (IOException e) {
            LOGGER.error("Error opening the browser to the Privacy Policy page of the AI provider.", e);
            dialogService.showErrorDialogAndWait(e);
        }
    }
}
