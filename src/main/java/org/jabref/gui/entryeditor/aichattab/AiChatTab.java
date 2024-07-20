package org.jabref.gui.entryeditor.aichattab;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.ai.components.apikeymissing.ApiKeyMissingComponent;
import org.jabref.gui.ai.components.errorstate.ErrorStateComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiGenerateEmbeddingsTask;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.events.DocumentIngestedEvent;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;

public class AiChatTab extends EntryEditorTab {
    private final LibraryTabContainer libraryTabContainer;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiService aiService;

    private final List<BibEntry> entriesUnderIngestion = new ArrayList<>();

    public AiChatTab(LibraryTabContainer libraryTabContainer,
                     DialogService dialogService,
                     PreferencesService preferencesService,
                     AiService aiService,
                     BibDatabaseContext bibDatabaseContext,
                     TaskExecutor taskExecutor) {
        this.libraryTabContainer = libraryTabContainer;
        this.dialogService = dialogService;
        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.taskExecutor = taskExecutor;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, preferencesService.getCitationKeyPatternPreferences());

        setText(Localization.lang("AI chat"));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));
        aiService.getEmbeddingsManager().registerListener(new FileIngestedListener());
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

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (!aiService.getPreferences().getEnableChatWithFiles()) {
            showPrivacyNotice(entry);
        } else if (aiService.getPreferences().getOpenAiToken().isEmpty()) {
            showApiKeyMissing();
        } else if (entry.getFiles().isEmpty()) {
            showErrorNoFiles();
        } else if (entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).noneMatch(FileUtil::isPDFFile)) {
            showErrorNotPdfs();
        } else if (!citationKeyIsValid(bibDatabaseContext, entry)) {
            tryToGenerateCitationKeyThenBind(entry);
        } else if (!aiService.getEmbeddingsManager().hasIngestedLinkedFiles(entry.getFiles())) {
            startIngesting(entry);
        } else {
            entriesUnderIngestion.remove(entry);
            bindToCorrectEntry(entry);
        }
    }

    private void bindToCorrectEntry(BibEntry entry) {
        AiChatTabWorking aiChatTabWorking = new AiChatTabWorking(aiService, entry, bibDatabaseContext, taskExecutor, dialogService);
        setContent(aiChatTabWorking.getNode());
    }

    private void showPrivacyNotice(BibEntry entry) {
        setContent(new PrivacyNoticeComponent(dialogService, aiService.getPreferences(), filePreferences, () -> {
            bindToEntry(entry);
        }));
    }

    private void showApiKeyMissing() {
        setContent(new ApiKeyMissingComponent(libraryTabContainer, dialogService));
    }

    private void showErrorNotIngested() {
        setContent(ErrorStateComponent.withSpinner(Localization.lang("Please wait"), Localization.lang("The embeddings of the file are currently being generated. Please wait, and at the end you will be able to chat.")));
    }

    private void showErrorNotPdfs() {
        setContent(new ErrorStateComponent(Localization.lang("Unable to chat"), Localization.lang("Only PDF files are supported.")));
    }

    private void showErrorNoFiles() {
        setContent(new ErrorStateComponent(Localization.lang("Unable to chat"), Localization.lang("Please attach at least one PDF file to enable chatting with PDF files.")));
    }

    private void tryToGenerateCitationKeyThenBind(BibEntry entry) {
        if (citationKeyGenerator.generateAndSetKey(entry).isEmpty()) {
            setContent(new ErrorStateComponent(Localization.lang("Unable to chat"), Localization.lang("Please provide a non-empty and unique citation key for this entry.")));
        } else {
            bindToEntry(entry);
        }
    }

    private static boolean citationKeyIsValid(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        return !hasEmptyCitationKey(bibEntry) && bibEntry.getCitationKey().map(key -> citationKeyIsUnique(bibDatabaseContext, key)).orElse(false);
    }

    private static boolean hasEmptyCitationKey(BibEntry bibEntry) {
        return bibEntry.getCitationKey().map(String::isEmpty).orElse(true);
    }

    private static boolean citationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) == 1;
    }

    private void startIngesting(BibEntry entry) {
        showErrorNotIngested();

        if (!entriesUnderIngestion.contains(entry)) {
            entriesUnderIngestion.add(entry);
            new AiGenerateEmbeddingsTask(entry.getFiles(), aiService.getEmbeddingsManager(), bibDatabaseContext, filePreferences, new SimpleBooleanProperty(false))
                    .onSuccess(res -> handleFocus())
                    .onFailure(this::showErrorWhileIngesting)
                    .executeWith(taskExecutor);
        }
    }

    private void showErrorWhileIngesting(Exception e) {
        setContent(ErrorStateComponent.withTextArea(Localization.lang("Unable to chat"), Localization.lang("Got error while processing the file:"), e.getMessage()));
        entriesUnderIngestion.remove(currentEntry);
        currentEntry.getFiles().stream().map(LinkedFile::getLink).forEach(link -> aiService.getEmbeddingsManager().removeDocument(link));
    }

    private class FileIngestedListener {
        @Subscribe
        public void listen(DocumentIngestedEvent event) {
             UiTaskExecutor.runInJavaFXThread(AiChatTab.this::handleFocus);
        }
    }
}
