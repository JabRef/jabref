package org.jabref.gui.ai.components.summary;

import java.nio.file.Path;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.privacynotice.AiPrivacyNoticeGuardedComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.summarization.SummariesStorage;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.ai.chatting.chathistory.ChatHistoryService.citationKeyIsValid;

public class SummaryComponent extends AiPrivacyNoticeGuardedComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryComponent.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiService aiService;

    public SummaryComponent(BibDatabaseContext bibDatabaseContext,
                            BibEntry entry, AiService aiService,
                            FilePreferences filePreferences,
                            CitationKeyPatternPreferences citationKeyPatternPreferences,
                            DialogService dialogService
    ) {
        super(aiService.getPreferences(), filePreferences, dialogService);

        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences);
        this.aiService = aiService;

        aiService.getSummariesService().summarize(entry, bibDatabaseContext).state().addListener(o -> rebuildUi());

        rebuildUi();
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

        return switch (processingInfo.state().get()) {
            case SUCCESS ->
                    showSummary(processingInfo.data().get());
            case ERROR ->
                    showErrorWhileSummarizing(processingInfo);
            case PROCESSING ->
                    showErrorNotSummarized();
        };
    }

    private Node showErrorWhileSummarizing(ProcessingInfo<BibEntry, SummariesStorage.SummarizationRecord> processingInfo) {
        LOGGER.error("Got an error while generating a summary for entry {}", entry.getCitationKey().orElse("<no citation key>"), processingInfo.exception().get());

        return ErrorStateComponent.withTextAreaAndButton(
                Localization.lang("Unable to chat"),
                Localization.lang("Got error while processing the file:"),
                processingInfo.exception().get().getLocalizedMessage(),
                Localization.lang("Regenerate"),
                () -> aiService.getSummariesService().regenerateSummary(entry, bibDatabaseContext)
        );
    }

    private Node showErrorNotSummarized() {
        return ErrorStateComponent.withSpinner(
                Localization.lang("Processing..."),
                Localization.lang("The attached file(s) are currently being processed by %0. Once completed, you will be able to see the summary.", aiService.getPreferences().getAiProvider().getLabel())
        );
    }

    private Node showSummary(SummariesStorage.SummarizationRecord summary) {
        return new SummaryShowingComponent(summary, () -> {
            if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                LOGGER.error("Bib database path is not set, but it was expected to be present. Unable to regenerate summary");
                return;
            }

            if (entry.getCitationKey().isEmpty()) {
                LOGGER.error("Citation key is not set, but it was expected to be present. Unable to regenerate summary");
                return;
            }

            aiService.getSummariesService().regenerateSummary(entry, bibDatabaseContext);
            // No need to rebuildUi(), because this class listens to the state of ProcessingInfo of the summary.
        });
    }
}
