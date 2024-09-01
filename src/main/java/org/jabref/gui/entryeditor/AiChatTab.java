package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatGuardedComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.ai.AiPreferences;

public class AiChatTab extends EntryEditorTab {
    private final BibDatabaseContext bibDatabaseContext;
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final CitationKeyGenerator citationKeyGenerator;
    private final TaskExecutor taskExecutor;

    private Optional<BibEntry> previousBibEntry = Optional.empty();

    public AiChatTab(BibDatabaseContext bibDatabaseContext,
                     AiService aiService,
                     DialogService dialogService,
                     PreferencesService preferencesService,
                     TaskExecutor taskExecutor
    ) {
        this.bibDatabaseContext = bibDatabaseContext;

        this.aiService = aiService;
        this.dialogService = dialogService;

        this.aiPreferences = preferencesService.getAiPreferences();
        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, preferencesService.getCitationKeyPatternPreferences());

        this.taskExecutor = taskExecutor;

        setText(Localization.lang("AI chat"));
        setTooltip(new Tooltip(Localization.lang("Chat with AI about content of attached file(s)")));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    /**
     * @implNote Method similar to {@link AiSummaryTab#bindToEntry(BibEntry)}
     */
    @Override
    protected void bindToEntry(BibEntry entry) {
        previousBibEntry.ifPresent(previousBibEntry ->
                aiService.getChatHistoryService().closeChatHistoryForEntry(previousBibEntry));

        previousBibEntry = Optional.of(entry);

        if (!aiPreferences.getEnableAi()) {
            showPrivacyNotice(entry);
        } else if (entry.getFiles().isEmpty()) {
            showErrorNoFiles();
        } else if (entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).noneMatch(FileUtil::isPDFFile)) {
            showErrorNotPdfs();
        } else if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            tryToGenerateCitationKeyThenBind(entry);
        } else {
            bindToCorrectEntry(entry);
        }
    }

    private void showPrivacyNotice(BibEntry entry) {
        setContent(new PrivacyNoticeComponent(aiPreferences, () -> bindToEntry(entry), filePreferences, dialogService));
    }

    private void showErrorNotPdfs() {
        setContent(
                new ErrorStateComponent(
                        Localization.lang("Unable to chat"),
                        Localization.lang("Only PDF files are supported.")
                )
        );
    }

    private void showErrorNoFiles() {
        setContent(
                new ErrorStateComponent(
                        Localization.lang("Unable to chat"),
                        Localization.lang("Please attach at least one PDF file to enable chatting with PDF file(s).")
                )
        );
    }

    private void tryToGenerateCitationKeyThenBind(BibEntry entry) {
        if (citationKeyGenerator.generateAndSetKey(entry).isEmpty()) {
            setContent(
                    new ErrorStateComponent(
                            Localization.lang("Unable to chat"),
                            Localization.lang("Please provide a non-empty and unique citation key for this entry.")
                    )
            );
        } else {
            bindToEntry(entry);
        }
    }

    private void bindToCorrectEntry(BibEntry entry) {
        // We omit the localization here, because it is only a chat with one entry in the {@link EntryEditor}.
        // See documentation for {@link AiChatGuardedComponent#name}.
        StringProperty chatName = new SimpleStringProperty("entry " + entry.getCitationKey().orElse("<no citation key>"));
        entry.getCiteKeyBinding().addListener((observable, oldValue, newValue) -> chatName.setValue("entry " + newValue));

        setContent(new AiChatGuardedComponent(
                chatName,
                aiService.getChatHistoryService().getChatHistoryForEntry(entry),
                bibDatabaseContext,
                FXCollections.observableArrayList(new ArrayList<>(List.of(entry))),
                aiService,
                dialogService,
                aiPreferences,
                filePreferences,
                taskExecutor
        ));
    }
}
