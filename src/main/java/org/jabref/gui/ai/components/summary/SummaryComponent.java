package org.jabref.gui.ai.components.summary;

import java.nio.file.Path;
import java.util.List;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.guards.EmbeddingModelGuard;
import org.jabref.gui.ai.components.guards.privacynotice.AiPrivacyNoticeGuard;
import org.jabref.gui.util.components.ErrorStateComponent;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.guards.BibDatabasePathPresentGuard;
import org.jabref.gui.util.guards.CitationKeyGuard;
import org.jabref.gui.util.guards.EntryFilesArePdfsGuard;
import org.jabref.gui.util.guards.EntryFilesGuard;
import org.jabref.gui.util.guards.GuardedComponent;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.summarization.Summary;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryComponent extends GuardedComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryComponent.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiService aiService;
    private final AiPreferences aiPreferences;

    public SummaryComponent(BibDatabaseContext bibDatabaseContext,
                            BibEntry entry,
                            AiService aiService,
                            AiPreferences aiPreferences,
                            ExternalApplicationsPreferences externalApplicationsPreferences,
                            CitationKeyPatternPreferences citationKeyPatternPreferences,
                            DialogService dialogService
    ) {
        super(List.of(
                new AiPrivacyNoticeGuard(aiPreferences, externalApplicationsPreferences, dialogService),
                new EmbeddingModelGuard(aiService),
                new BibDatabasePathPresentGuard(bibDatabaseContext),
                new EntryFilesGuard(entry),
                new EntryFilesArePdfsGuard(entry),
                new CitationKeyGuard(bibDatabaseContext, entry)
        ));

        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences);
        this.aiService = aiService;
        this.aiPreferences = aiPreferences;

        aiService.getSummariesService().summarize(entry, bibDatabaseContext).stateProperty().addListener(o -> checkGuards());

        checkGuards();
    }

    @Override
    protected Node showGuardedComponent() {
        ProcessingInfo<BibEntry, Summary> processingInfo = aiService.getSummariesService().summarize(entry, bibDatabaseContext);

        return switch (processingInfo.getState()) {
            case SUCCESS -> {
                assert processingInfo.getData().isPresent(); // When the state is SUCCESS, the data must be present.
                yield showSummary(processingInfo.getData().get());
            }
            case ERROR ->
                    showErrorWhileSummarizing(processingInfo);
            case PROCESSING,
                 STOPPED ->
                    showErrorNotSummarized();
        };
    }

    private Node showErrorWhileSummarizing(ProcessingInfo<BibEntry, Summary> processingInfo) {
        assert processingInfo.getException().isPresent(); // When the state is ERROR, the exception must be present.

        LOGGER.error("Got an error while generating a summary for entry {}", entry.getCitationKey().orElse("<no citation key>"), processingInfo.getException().get());

        return ErrorStateComponent.withTextAreaAndButton(
                Localization.lang("Unable to chat"),
                Localization.lang("Got error while processing the file:"),
                processingInfo.getException().get().getLocalizedMessage(),
                Localization.lang("Regenerate"),
                () -> aiService.getSummariesService().regenerateSummary(entry, bibDatabaseContext)
        );
    }

    private Node showErrorNotSummarized() {
        return ErrorStateComponent.withSpinner(
                Localization.lang("Processing..."),
                Localization.lang("The attached file(s) are currently being processed by %0. Once completed, you will be able to see the summary.", aiPreferences.getAiProvider().getLabel())
        );
    }

    private Node showSummary(Summary summary) {
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
