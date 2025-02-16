package org.jabref.gui.ai.components.privacynotice;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

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

    @FXML private VBox text;
    @FXML private GridPane aiPolicies;
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
        addPrivacyHyperlink(aiPolicies, AiProvider.OPEN_AI);
        addPrivacyHyperlink(aiPolicies, AiProvider.MISTRAL_AI);
        addPrivacyHyperlink(aiPolicies, AiProvider.GEMINI);
        addPrivacyHyperlink(aiPolicies, AiProvider.HUGGING_FACE);
        addPrivacyHyperlink(aiPolicies, AiProvider.GPT4ALL);

        String newEmbeddingModelText = embeddingModelText.getText().replaceAll("%0", aiPreferences.getEmbeddingModel().sizeInfo());
        embeddingModelText.setText(newEmbeddingModelText);

        // Because of the https://bugs.openjdk.org/browse/JDK-8090400 bug, the text in the privacy policy cannot be
        // fully wrapped.

        DoubleBinding textWidth = Bindings.subtract(this.widthProperty(), 88d);
        text.getChildren().forEach(child -> {
            if (child instanceof Text line) {
                line.wrappingWidthProperty().bind(textWidth);
            }
        });
        aiPolicies.prefWidthProperty().bind(textWidth);
        embeddingModelText.wrappingWidthProperty().bind(textWidth);
    }

    private void addPrivacyHyperlink(GridPane gridPane, AiProvider aiProvider) {
        int row = gridPane.getRowCount();
        Label aiName = new Label(aiProvider.getLabel());
        gridPane.add(aiName, 0, row);

        Hyperlink hyperlink = new Hyperlink(aiProvider.getPrivacyPolicyUrl());
        hyperlink.setWrapText(true);
        // hyperlink.setFont(aiName.getFont());
        hyperlink.setOnAction(event -> openBrowser(aiProvider.getApiUrl()));
        gridPane.add(hyperlink, 1, row);
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
