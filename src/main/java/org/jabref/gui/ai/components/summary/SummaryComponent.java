package org.jabref.gui.ai.components.summary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.privacynotice.AiPrivacyNoticeGuardedComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
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

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.ai.chatting.chathistory.ChatHistoryService.citationKeyIsValid;

public class SummaryComponent extends AiPrivacyNoticeGuardedComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryComponent.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiService aiService;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    public SummaryComponent(BibDatabaseContext bibDatabaseContext, BibEntry entry, AiService aiService, PreferencesService preferencesService, DialogService dialogService, TaskExecutor taskExecutor) {
        super(aiService.getPreferences(), preferencesService.getFilePreferences(), dialogService);

        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, preferencesService.getCitationKeyPatternPreferences());
        this.aiService = aiService;
        this.filePreferences = preferencesService.getFilePreferences();
        this.taskExecutor = taskExecutor;
    }

    @Override
    protected Node showPrivacyPolicyGuardedContent() {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            return showErrorNoDatabasePath();
        } else if (entry.getFiles().isEmpty()) {
            return showErrorNoFiles();
        } else if (entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).noneMatch(FileUtil::isPDFFile)) {
            return showErrorNotPdfs();
        } else if (entry.getCitationKey().isEmpty() || !citationKeyIsValid(bibDatabaseContext, entry)) {
            // There is no need for additional check `entry.getCitationKey().isEmpty()` because method `citationKeyIsValid`,
            // will check this. But with this call the linter is happy for the next expression in else if.
            return tryToGenerateCitationKeyThenBind(entry);
        } else {
            return tryToShowSummary();
        }
    }

    private Node showErrorNoDatabasePath() {
        return new ErrorStateComponent(
                Localization.lang("Unable to chat"),
                Localization.lang("The path of the current library is not set, but it is required for summarization")
        );
    }

    private Node showErrorNotPdfs() {
        return new ErrorStateComponent(
                Localization.lang("Unable to chat"),
                Localization.lang("Only PDF files are supported.")
        );
    }

    private Node showErrorNoFiles() {
        return new ErrorStateComponent(
                Localization.lang("Unable to chat"),
                Localization.lang("Please attach at least one PDF file to enable chatting with PDF file(s).")
        );
    }

    private Node tryToGenerateCitationKeyThenBind(BibEntry entry) {
        if (citationKeyGenerator.generateAndSetKey(entry).isEmpty()) {
            return new ErrorStateComponent(
                    Localization.lang("Unable to chat"),
                    Localization.lang("Please provide a non-empty and unique citation key for this entry.")
            );
        } else {
            return showPrivacyPolicyGuardedContent();
        }
    }

    private Node tryToShowSummary() {
        ProcessingInfo<BibEntry, SummariesStorage.SummarizationRecord> processingInfo = aiService.getSummariesService().summarize(entry, bibDatabaseContext);

        switch (processingInfo.state().get()) {
            case SUCCESS:
                return showSummary(processingInfo.data());
            case ERROR:
                return showErrorNotSummarized();
            case RUNNING:
                return startGeneratingSummary(entry);
        }

        if (processingInfo.state() == ProcessingState.SUCCESS) {
            return showSummary(processingInfo.data());
        } else if (pro)
    }

    private void showErrorWhileSummarizing(Exception e) {
        LOGGER.error("Got an error while generating a summary for entry {}", entry.getCitationKey().orElse("<no citation key>"), e);

        setContent(
                ErrorStateComponent.withTextAreaAndButton(
                        Localization.lang("Unable to chat"),
                        Localization.lang("Got error while processing the file:"),
                        e.getMessage(),
                        Localization.lang("Regenerate"),
                        () -> bindToEntry(currentEntry)
                )
        );
    }


    private Node startGeneratingSummary(BibEntry entry) {
        assert entry.getCitationKey().isPresent();

        if (!entriesUnderSummarization.contains(entry)) {
            entriesUnderSummarization.add(entry);

            new GenerateSummaryTask(bibDatabaseContext, entry.getCitationKey().get(), entry.getFiles(), aiService, filePreferences)
                    .onSuccess(res -> rebuildUi())
                    .onFailure(e -> rebuildUi())
                    .executeWith(taskExecutor);
        }

        return showErrorNotSummarized();
    }

    private Node showErrorNotSummarized() {
        return ErrorStateComponent.withSpinner(
                Localization.lang("Processing..."),
                Localization.lang("The attached file(s) are currently being processed by %0. Once completed, you will be able to see the summary.", aiService.getPreferences().getAiProvider().getLabel())
        );
    }


    private Node bindToCorrectEntry(SummariesStorage.SummarizationRecord summary) {
        entriesUnderSummarization.remove(entry);

        return new SummaryShowingComponent(summary, () -> {
            if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                LOGGER.error("Bib database path is not set, but it was expected to be present. Unable to regenerate summary");
                return;
            }

            if (entry.getCitationKey().isEmpty()) {
                LOGGER.error("Citation key is not set, but it was expected to be present. Unable to regenerate summary");
                return;
            }

            aiService.getSummariesStorage().clear(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());
            rebuildUi();
        });
    }
}
