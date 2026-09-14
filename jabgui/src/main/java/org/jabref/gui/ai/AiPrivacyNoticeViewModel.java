package org.jabref.gui.ai;

import java.io.IOException;
import java.util.OptionalLong;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.logic.ai.embedding.EmbeddingModelMetadata;
import org.jabref.logic.ai.embedding.EmbeddingModelMetadataService;
import org.jabref.logic.ai.preferences.AiPreferences;

import org.apache.commons.io.FileUtils;
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
    private final EmbeddingModelMetadataService embeddingModelMetadataService;

    public AiPrivacyNoticeViewModel(
            AiPreferences aiPreferences,
            ExternalApplicationsPreferences externalApplicationsPreferences,
            EntryEditorPreferences entryEditorPreferences,
            GroupsPreferences groupsPreferences,
            DialogService dialogService,
            EmbeddingModelMetadataService embeddingModelMetadataService
    ) {
        this.aiPreferences = aiPreferences;
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.entryEditorPreferences = entryEditorPreferences;
        this.groupsPreferences = groupsPreferences;
        this.dialogService = dialogService;
        this.embeddingModelMetadataService = embeddingModelMetadataService;

        setupBindings();
    }

    private void setupBindings() {
        embeddingModelSize.bind(aiPreferences.embeddingModelProperty().map(modelName ->
                embeddingModelMetadataService
                        .getMetadata(modelName)
                        .map(EmbeddingModelMetadata::downloadSizeBytes)
                        .filter(OptionalLong::isPresent)
                        .map(OptionalLong::getAsLong)
                        .map(FileUtils::byteCountToDisplaySize)
                        .orElse("")));
    }

    public void onPrivacyAgree() {
        aiPreferences.setAiFeaturesEnabledCurrently(true);
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
        entryEditorPreferences.setTabVisible(EntryEditorTabModel.BuiltIn.AI_CHAT, false);
        entryEditorPreferences.setTabVisible(EntryEditorTabModel.BuiltIn.AI_SUMMARY, false);
        groupsPreferences.setShowAiChatButton(false);
        aiPreferences.setAiFeaturesEnabledCurrently(false);
    }

    public ReadOnlyStringProperty embeddingModelSizeProperty() {
        return embeddingModelSize;
    }
}
