package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatComponent;
import org.jabref.gui.ai.components.errorstate.ErrorStateComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.embeddings.FullyIngestedDocumentsTracker;
import org.jabref.logic.ai.models.JabRefEmbeddingModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;

public class AiChatTab extends EntryEditorTab {
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final AiService aiService;

    public AiChatTab(DialogService dialogService,
                     PreferencesService preferencesService,
                     AiService aiService,
                     BibDatabaseContext bibDatabaseContext,
                     TaskExecutor taskExecutor) {
        this.dialogService = dialogService;

        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.taskExecutor = taskExecutor;

        setText(Localization.lang("AI chat"));
        setTooltip(new Tooltip(Localization.lang("Chat with AI about content of attached file(s)")));

        aiService.getEmbeddingsManager().registerListener(this);
        aiService.getEmbeddingModel().registerListener(this);
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    @Override
    protected void handleFocus() {
        if (currentEntry != null) {
            bindToEntry(currentEntry);
        }
    }

    /**
     * @implNote Method similar to {@link AiSummaryTab#bindToEntry(BibEntry)}
     */
    @Override
    protected void bindToEntry(BibEntry entry) {
        if (currentEntry != null) {
            aiService.getChatHistoryService().closeChatHistoryForEntry(currentEntry);
        }

        if (!aiService.getPreferences().getEnableAi()) {
            showPrivacyNotice(entry);
        } else if (!aiService.getEmbeddingModel().isPresent()) {
            if (aiService.getEmbeddingModel().hadErrorWhileBuildingModel()) {
                showErrorWhileBuildingEmbeddingModel();
            } else {
                showBuildingEmbeddingModel();
            }
        } else {
            bindToCorrectEntry(entry);
        }
    }

    private void showPrivacyNotice(BibEntry entry) {
        setContent(new PrivacyNoticeComponent(dialogService, aiService.getPreferences(), filePreferences, () -> {
            bindToEntry(entry);
        }));
    }

    private void showErrorWhileBuildingEmbeddingModel() {
        setContent(
                ErrorStateComponent.withTextAreaAndButton(
                        Localization.lang("Unable to chat"),
                        Localization.lang("An error occurred while building the embedding model"),
                        aiService.getEmbeddingModel().getErrorWhileBuildingModel(),
                        Localization.lang("Rebuild"),
                        () -> aiService.getEmbeddingModel().startRebuildingTask()
                )
        );
    }

    public void showBuildingEmbeddingModel() {
        setContent(
                ErrorStateComponent.withSpinner(
                        Localization.lang("Downloading..."),
                        Localization.lang("Downloading embedding model... Afterward, you will be able to chat with your files.")
                )
        );
    }

    private void bindToCorrectEntry(BibEntry entry) {
        setContent(
                new AiChatComponent(
                    aiService,
                    "entry " + entry.getCitationKey().orElse("<no citation key>"),
                    aiService.getChatHistoryService().getChatHistoryForEntry(entry),
                    FXCollections.observableArrayList(new ArrayList<>(List.of(entry))),
                    bibDatabaseContext,
                    dialogService,
                    taskExecutor
                )
        );
    }

    @Subscribe
    public void listen(FullyIngestedDocumentsTracker.DocumentIngestedEvent event) {
        UiTaskExecutor.runInJavaFXThread(AiChatTab.this::handleFocus);
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuiltEvent event) {
        UiTaskExecutor.runInJavaFXThread(AiChatTab.this::handleFocus);
    }

    @Subscribe
    public void listen(JabRefEmbeddingModel.EmbeddingModelBuildingErrorEvent event) {
        UiTaskExecutor.runInJavaFXThread(AiChatTab.this::handleFocus);
    }
}
