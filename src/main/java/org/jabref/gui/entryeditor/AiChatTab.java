package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.ai.components.aichat.AiChatComponent;
import org.jabref.gui.ai.components.apikeymissing.ApiKeyMissingComponent;
import org.jabref.gui.ai.components.errorstate.ErrorStateComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiChatLogic;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.GenerateEmbeddingsTask;
import org.jabref.logic.ai.chathistory.AiChatHistory;
import org.jabref.logic.ai.chathistory.InMemoryAiChatHistory;
import org.jabref.logic.ai.embeddings.FullyIngestedDocumentsTracker;
import org.jabref.logic.ai.models.JabRefEmbeddingModel;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatTab extends EntryEditorTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatTab.class);

    private final LibraryTabContainer libraryTabContainer;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final CitationKeyGenerator citationKeyGenerator;
    private final PreferencesService preferencesService;
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
        this.preferencesService = preferencesService;

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

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (!aiService.getPreferences().getEnableAi()) {
            showPrivacyNotice(entry);
        } else if (aiService.getPreferences().getSelectedApiKey(preferencesService).isEmpty()) {
            showApiKeyMissing();
        } else if (entry.getFiles().isEmpty()) {
            showErrorNoFiles();
        } else if (entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).noneMatch(FileUtil::isPDFFile)) {
            showErrorNotPdfs();
        } else if (!citationKeyIsValid(bibDatabaseContext, entry)) {
            tryToGenerateCitationKeyThenBind(entry);
        } else if (!aiService.getEmbeddingModel().isPresent()) {
            if (aiService.getEmbeddingModel().hadErrorWhileBuildingModel()) {
                showErrorWhileBuildingEmbeddingModel();
            } else {
                showBuildingEmbeddingModel();
            }
        } else if (!aiService.getEmbeddingsManager().hasIngestedLinkedFiles(entry.getFiles())) {
            startIngesting(entry);
        } else {
            entriesUnderIngestion.remove(entry);
            bindToCorrectEntry(entry);
        }
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
        setContent(
                ErrorStateComponent.withSpinner(
                        Localization.lang("Processing..."),
                        Localization.lang("The embeddings of the file(s) are currently being generated. Please wait, and at the end you will be able to chat.")
                )
        );
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
        // This method should be called if entry is fully prepared for chatting.
        assert entry.getCitationKey().isPresent();

        showErrorNotIngested();

        if (!entriesUnderIngestion.contains(entry)) {
            entriesUnderIngestion.add(entry);

            new GenerateEmbeddingsTask(entry.getCitationKey().get(), entry.getFiles(), aiService.getEmbeddingsManager(), bibDatabaseContext, filePreferences)
                    .onSuccess(res -> handleFocus())
                    .onFailure(this::showErrorWhileIngesting)
                    .executeWith(taskExecutor);
        }
    }

    private void showErrorWhileIngesting(Exception e) {
        LOGGER.error("Got an error while generating embeddings for entry {}", currentEntry.getCitationKey(), e);

        setContent(ErrorStateComponent.withTextArea(Localization.lang("Unable to chat"), Localization.lang("Got error while processing the file:"), e.getMessage()));

        entriesUnderIngestion.remove(currentEntry);

        currentEntry.getFiles().stream().map(LinkedFile::getLink).forEach(link -> aiService.getEmbeddingsManager().removeDocument(link));
    }

    private void bindToCorrectEntry(BibEntry entry) {
        assert entry.getCitationKey().isPresent();

        AiChatHistory aiChatHistory = getAiChatHistory(aiService, entry, bibDatabaseContext);

        AiChatLogic aiChatLogic = AiChatLogic.forBibEntry(aiService, aiChatHistory, entry);

        Node content = new AiChatComponent(aiService.getPreferences(), aiChatLogic, entry.getCitationKey().get(), dialogService, taskExecutor);

        setContent(content);
    }

    private static AiChatHistory getAiChatHistory(AiService aiService, BibEntry entry, BibDatabaseContext bibDatabaseContext) {
        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();

        if (databasePath.isEmpty() || entry.getCitationKey().isEmpty()) {
            LOGGER.warn("AI chat is constructed, but the database path is empty. Cannot store chat history");
            return new InMemoryAiChatHistory();
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.warn("AI chat is constructed, but the entry citation key is empty. Cannot store chat history");
            return new InMemoryAiChatHistory();
        } else {
            return aiService.getChatHistoryManager().getChatHistory(databasePath.get(), entry.getCitationKey().get());
        }
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
