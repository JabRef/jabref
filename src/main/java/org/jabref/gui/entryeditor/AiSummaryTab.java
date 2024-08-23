package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.ai.components.summary.SummaryComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.summarization.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.SummariesStorage;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.ai.AiApiKeyProvider;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSummaryTab extends EntryEditorTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiSummaryTab.class);

    private final LibraryTabContainer libraryTabContainer;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiApiKeyProvider aiApiKeyProvider;
    private final AiService aiService;

    private final List<BibEntry> entriesUnderSummarization = new ArrayList<>();

    public AiSummaryTab(LibraryTabContainer libraryTabContainer,
                        DialogService dialogService,
                        PreferencesService preferencesService,
                        AiApiKeyProvider aiApiKeyProvider,
                        AiService aiService,
                        BibDatabaseContext bibDatabaseContext,
                        TaskExecutor taskExecutor) {
        this.libraryTabContainer = libraryTabContainer;
        this.dialogService = dialogService;
        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.aiApiKeyProvider = aiApiKeyProvider;
        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.taskExecutor = taskExecutor;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, preferencesService.getCitationKeyPatternPreferences());

        setText(Localization.lang("AI summary"));
        setTooltip(new Tooltip(Localization.lang("AI-generated summary of attached file(s)")));

        aiService.getSummariesStorage().registerListener(new SummarySetListener());
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiSummaryTab();
    }

    @Override
    protected void handleFocus() {
        if (currentEntry != null) {
            bindToEntry(currentEntry);
        }
    }

    /**
     * @implNote Method similar to {@link AiChatTab#bindToEntry(BibEntry)}
     */
    @Override
    protected void bindToEntry(BibEntry entry) {
        if (!aiService.getPreferences().getEnableAi()) {
            showPrivacyNotice(entry);
        } else if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            showErrorNoDatabasePath();
        } else if (entry.getFiles().isEmpty()) {
            showErrorNoFiles();
        } else if (entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).noneMatch(FileUtil::isPDFFile)) {
            showErrorNotPdfs();
        } else if (entry.getCitationKey().isEmpty() || !citationKeyIsValid(bibDatabaseContext, entry)) {
            // There is no need for additional check `entry.getCitationKey().isEmpty()` because method `citationKeyIsValid`,
            // will check this. But with this call the linter is happy for the next expression in else if.
            tryToGenerateCitationKeyThenBind(entry);
        } else {
            Optional<SummariesStorage.SummarizationRecord> summary = aiService.getSummariesStorage().get(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());
            if (summary.isEmpty()) {
                startGeneratingSummary(entry);
            } else {
                bindToCorrectEntry(summary.get());
            }
        }
    }

    private void showPrivacyNotice(BibEntry entry) {
        setContent(new PrivacyNoticeComponent(aiService.getPreferences(), () -> {
            bindToEntry(entry);
        }, filePreferences, dialogService));
    }

    private void showErrorNoDatabasePath() {
        setContent(
                new ErrorStateComponent(
                        Localization.lang("Unable to chat"),
                        Localization.lang("The path of the current library is not set, but it is required for summarization")
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

    private static boolean citationKeyIsValid(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        return !hasEmptyCitationKey(bibEntry) && bibEntry.getCitationKey().map(key -> citationKeyIsUnique(bibDatabaseContext, key)).orElse(false);
    }

    private static boolean hasEmptyCitationKey(BibEntry bibEntry) {
        return bibEntry.getCitationKey().map(String::isEmpty).orElse(true);
    }

    private static boolean citationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) == 1;
    }

    private void startGeneratingSummary(BibEntry entry) {
        assert entry.getCitationKey().isPresent();

        showErrorNotSummarized();

        if (!entriesUnderSummarization.contains(entry)) {
            entriesUnderSummarization.add(entry);

            new GenerateSummaryTask(bibDatabaseContext, entry.getCitationKey().get(), entry.getFiles(), aiService, filePreferences)
                    .onSuccess(res -> handleFocus())
                    .onFailure(this::showErrorWhileSummarizing)
                    .executeWith(taskExecutor);
        }
    }

    private void showErrorWhileSummarizing(Exception e) {
        LOGGER.error("Got an error while generating a summary for entry {}", currentEntry.getCitationKey(), e);

        setContent(
                ErrorStateComponent.withTextAreaAndButton(
                        Localization.lang("Unable to chat"),
                        Localization.lang("Got error while processing the file:"),
                        e.getMessage(),
                        Localization.lang("Regenerate"),
                        () -> bindToEntry(currentEntry)
                )
        );

        entriesUnderSummarization.remove(currentEntry);
    }

    private void showErrorNotSummarized() {
        setContent(
                ErrorStateComponent.withSpinner(
                        Localization.lang("Processing..."),
                        Localization.lang("The attached file(s) are currently being processed by %0. Once completed, you will be able to see the summary.", aiService.getPreferences().getAiProvider().getLabel())
                )
        );
    }

    private void bindToCorrectEntry(SummariesStorage.SummarizationRecord summary) {
        entriesUnderSummarization.remove(currentEntry);

        SummaryComponent summaryComponent = new SummaryComponent(summary, () -> {
            if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                LOGGER.error("Bib database path is not set, but it was expected to be present. Unable to regenerate summary");
                return;
            }

            if (currentEntry.getCitationKey().isEmpty()) {
                LOGGER.error("Citation key is not set, but it was expected to be present. Unable to regenerate summary");
                return;
            }

            aiService.getSummariesStorage().clear(bibDatabaseContext.getDatabasePath().get(), currentEntry.getCitationKey().get());
            bindToEntry(currentEntry);
        });

        setContent(summaryComponent);
    }

    private class SummarySetListener {
        @Subscribe
        public void listen(SummariesStorage.SummarySetEvent event) {
            UiTaskExecutor.runInJavaFXThread(AiSummaryTab.this::handleFocus);
        }
    }
}
