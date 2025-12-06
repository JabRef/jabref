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
import org.jabref.gui.StateManager;
import org.jabref.gui.ai.components.aichat.AiChatGuardedComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;

public class AiChatTab extends EntryEditorTab {
    private final AiService aiService;
    private final DialogService dialogService;
    private final AiPreferences aiPreferences;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final AdaptVisibleTabs adaptVisibleTabs;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    private Optional<BibEntry> previousBibEntry = Optional.empty();

    public AiChatTab(AiService aiService,
                     StateManager stateManager,
                     TaskExecutor taskExecutor,
                     GuiPreferences preferences,
                     BibEntryTypesManager entryTypesManager,
                     DialogService dialogService,
                     AdaptVisibleTabs adaptVisibleTabs) {
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;

        this.entryEditorPreferences = preferences.getEntryEditorPreferences();
        this.citationKeyPatternPreferences = preferences.getCitationKeyPatternPreferences();
        this.aiPreferences = preferences.getAiPreferences();
        this.fieldPreferences = preferences.getFieldPreferences();
        this.externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();

        this.entryTypesManager = entryTypesManager;

        this.dialogService = dialogService;
        this.adaptVisibleTabs = adaptVisibleTabs;

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
        previousBibEntry.ifPresent(previousBibEntry -> aiService.getChatHistoryService().closeChatHistoryForEntry(previousBibEntry));
        previousBibEntry = Optional.of(entry);
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().orElse(new BibDatabaseContext());

        if (!aiPreferences.getEnableAi()) {
            showPrivacyNotice(entry);
        } else if (entry.getFiles().isEmpty()) {
            showErrorNoFiles();
        } else if (entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).noneMatch(FileUtil::isPDFFile)) {
            showErrorNotPdfs();
        } else if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            tryToGenerateCitationKeyThenBind(bibDatabaseContext, entry);
        } else {
            showChatPanel(bibDatabaseContext, entry);
        }
    }

    private void showPrivacyNotice(BibEntry entry) {
        setContent(new PrivacyNoticeComponent(aiPreferences, () -> bindToEntry(entry), externalApplicationsPreferences, dialogService, adaptVisibleTabs));
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

    private void tryToGenerateCitationKeyThenBind(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        CitationKeyGenerator citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences);
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

    private void showChatPanel(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        // We omit the localization here, because it is only a chat with one entry in the {@link EntryEditor}.
        // See documentation for {@link AiChatGuardedComponent#name}.
        StringProperty chatName = new SimpleStringProperty("entry " + entry.getCitationKey().orElse("<no citation key>"));
        entry.getCiteKeyBinding().addListener((observable, oldValue, newValue) -> chatName.setValue("entry " + newValue));

        setContent(new AiChatGuardedComponent(
                chatName,
                aiService.getChatHistoryService().getChatHistoryForEntry(bibDatabaseContext, entry),
                bibDatabaseContext,
                FXCollections.observableArrayList(new ArrayList<>(List.of(entry))),
                aiService,
                taskExecutor,
                aiPreferences,
                fieldPreferences,
                entryTypesManager,
                externalApplicationsPreferences,
                dialogService,
                adaptVisibleTabs

        ));
    }
}
