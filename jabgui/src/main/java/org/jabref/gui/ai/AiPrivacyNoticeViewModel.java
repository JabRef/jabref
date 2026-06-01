package org.jabref.gui.ai;

import java.io.IOException;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiPrivacyNoticeViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiPrivacyNoticeViewModel.class);

    private final StringProperty embeddingModelSize = new SimpleStringProperty("");

    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final GroupsPreferences groupsPreferences;
    private final DialogService dialogService;

    public AiPrivacyNoticeViewModel(
            AiPreferences aiPreferences,
            ExternalApplicationsPreferences externalApplicationsPreferences,
            EntryEditorPreferences entryEditorPreferences,
            GroupsPreferences groupsPreferences,
            DialogService dialogService
    ) {
        this.aiPreferences = aiPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.entryEditorPreferences = entryEditorPreferences;
        this.groupsPreferences = groupsPreferences;
        this.dialogService = dialogService;

        setupBindings();
    }

    private void setupBindings() {
        embeddingModelSize.bind(aiPreferences.embeddingModelProperty().map(PredefinedEmbeddingModel::sizeInfo));
    }

    public void onPrivacyAgree() {
        aiPreferences.setEnableAi(true);
    }

    public void openBrowser(String link) {
        try {
            NativeDesktop.openBrowser(link, externalApplicationsPreferences);
        } catch (IOException e) {
            LOGGER.error("Error opening the browser to the Privacy Policy page of the AI provider.", e);
            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void privacyDisagree() {
        entryEditorPreferences.setShouldShowAiChatTab(false);
        entryEditorPreferences.setShouldShowAiSummaryTab(false);
        groupsPreferences.setShowAiChatButton(false);
        aiPreferences.setEnableAi(false);
    }

    public ReadOnlyStringProperty embeddingModelSizeProperty() {
        return embeddingModelSize;
    }
}
