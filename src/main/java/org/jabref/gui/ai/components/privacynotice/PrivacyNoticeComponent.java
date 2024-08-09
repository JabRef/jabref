package org.jabref.gui.ai.components.privacynotice;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.preferences.ai.AiPreferences;
import org.jabref.preferences.FilePreferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivacyNoticeComponent extends ScrollPane {
    private final Logger LOGGER = LoggerFactory.getLogger(PrivacyNoticeComponent.class);

    @FXML private TextFlow openAiPrivacyTextFlow;
    @FXML private TextFlow mistralAiPrivacyTextFlow;
    @FXML private TextFlow huggingFacePrivacyTextFlow;
    @FXML private Text embeddingModelText;

    private final DialogService dialogService;

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;

    private final Runnable onIAgreeButtonClickCallback;

    public PrivacyNoticeComponent(DialogService dialogService, AiPreferences aiPreferences, FilePreferences filePreferences, Runnable onIAgreeButtonClickCallback) {
        this.dialogService = dialogService;
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;

        this.onIAgreeButtonClickCallback = onIAgreeButtonClickCallback;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        initPrivacyHyperlink(openAiPrivacyTextFlow, "https://openai.com/policies/privacy-policy/");
        initPrivacyHyperlink(mistralAiPrivacyTextFlow, "https://mistral.ai/terms/#privacy-policy");
        initPrivacyHyperlink(huggingFacePrivacyTextFlow, "https://huggingface.co/privacy");

        String newEmbeddingModelText = embeddingModelText.getText().replaceAll("%0", aiPreferences.getEmbeddingModel().sizeInfo());
        embeddingModelText.setText(newEmbeddingModelText);
    }

    private void initPrivacyHyperlink(TextFlow textFlow, String link) {
        if (textFlow.getChildren().isEmpty() || !(textFlow.getChildren().getFirst() instanceof Text text)) {
            return;
        }

        String[] stringArray = text.getText().split("%0");

        if (stringArray.length != 2) {
            return;
        }

        text.setText(stringArray[0]);

        Hyperlink hyperlink = new Hyperlink(link);
        hyperlink.setFont(text.getFont());
        hyperlink.setOnAction(event -> {
            openBrowser(link);
        });

        textFlow.getChildren().add(hyperlink);

        Text postText = new Text(stringArray[1]);
        postText.setFont(text.getFont());
        textFlow.getChildren().add(postText);
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
            JabRefDesktop.openBrowser(link, filePreferences);
        } catch (IOException e) {
            LOGGER.error("Error opening the browser to AI provider's privacy policy page.", e);
            dialogService.showErrorDialogAndWait(e);
        }
    }
}
