package org.jabref.gui.entryeditor.aichattab.components.privacynotice;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.FilePreferences;

import com.airhacks.afterburner.views.ViewLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivacyNoticeComponent extends Pane {
    private final Logger LOGGER = LoggerFactory.getLogger(PrivacyNoticeComponent.class);

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
    private void onPrivacyHyperlinkClick() {
        try {
            JabRefDesktop.openBrowser("https://openai.com/policies/privacy-policy/", filePreferences);
        } catch (IOException e) {
            LOGGER.error("Error opening the browser to OpenAI privacy policy page.", e);
            dialogService.showErrorDialogAndWait(e);
        }
    }

    @FXML
    private void onIAgreeButtonClick() {
        aiPreferences.setEnableChatWithFiles(true);
        onIAgreeButtonClickCallback.run();
    }
}
