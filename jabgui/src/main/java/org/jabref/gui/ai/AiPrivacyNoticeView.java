package org.jabref.gui.ai;

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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.URLs;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.model.ai.llm.AiProvider;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class AiPrivacyNoticeView extends ScrollPane {
    @FXML private VBox text;
    @FXML private GridPane aiPolicies;
    @FXML private Text embeddingModelText;

    @Inject private GuiPreferences preferences;
    @Inject private DialogService dialogService;

    private AiPrivacyNoticeViewModel viewModel;

    public AiPrivacyNoticeView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiPrivacyNoticeViewModel(
                preferences.getAiPreferences(),
                preferences.getExternalApplicationsPreferences(),
                preferences.getEntryEditorPreferences(),
                preferences.getGroupsPreferences(),
                dialogService
        );

        setupBindings();
        setupUi();
    }

    private void setupBindings() {
        DoubleBinding textWidth = Bindings.subtract(this.widthProperty(), 88d);
        text.getChildren().forEach(child -> {
            if (child instanceof Text line) {
                line.wrappingWidthProperty().bind(textWidth);
            }
        });

        aiPolicies.prefWidthProperty().bind(textWidth);
        embeddingModelText.wrappingWidthProperty().bind(textWidth);
    }

    private void setupUi() {
        addPrivacyHyperlink(aiPolicies, AiProvider.OPEN_AI);
        addPrivacyHyperlink(aiPolicies, AiProvider.MISTRAL_AI);
        addPrivacyHyperlink(aiPolicies, AiProvider.GEMINI);
        addPrivacyHyperlink(aiPolicies, AiProvider.HUGGING_FACE);

        // Note: Ideally, this should be bound to update automatically if the size changes but keeping the original logic for text replacement here.
        String embeddingTemplate = embeddingModelText.getText();
        String replaced = embeddingTemplate.replaceAll("%0", viewModel.embeddingModelSizeProperty().get());
        embeddingModelText.setText(replaced);
    }

    private void addPrivacyHyperlink(GridPane gridPane, AiProvider aiProvider) {
        int row = gridPane.getRowCount();
        Label aiName = new Label(AiNamingUtils.getDisplayName(aiProvider));
        gridPane.add(aiName, 0, row);

        Hyperlink hyperlink = new Hyperlink(aiProvider.getPrivacyPolicyUrl());
        hyperlink.setWrapText(true);
        hyperlink.setOnAction(_ -> viewModel.openBrowser(aiProvider.getApiUrl()));
        gridPane.add(hyperlink, 1, row);
    }

    @FXML
    private void onDjlLinkClick() {
        viewModel.openBrowser(URLs.DJL_PRIVACY_POLICY_URL);
    }

    @FXML
    private void onPrivacyAgree() {
        viewModel.onPrivacyAgree();
    }

    // [impl->req~ai.chat.entries.hide-tab~1]
    // [impl->req~ai.chat.groups.hide-context-menu~1]
    @FXML
    private void onPrivacyDisagree() {
        viewModel.privacyDisagree();
    }
}
