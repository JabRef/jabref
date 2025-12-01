package org.jabref.gui.ai.components.summary;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.privacynotice.AiPrivacyNoticeGuardedComponent;
import org.jabref.gui.ai.components.util.errorstate.ErrorStateComponent;
import org.jabref.gui.entryeditor.AdaptVisibleTabs;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
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
import org.jabref.model.entry.field.Field;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.StandardFileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;



import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummaryComponent extends AiPrivacyNoticeGuardedComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryComponent.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;
    private final CitationKeyGenerator citationKeyGenerator;
    private final AiService aiService;
    private final AiPreferences aiPreferences;
    private final DialogService dialogService;

    public SummaryComponent(BibDatabaseContext bibDatabaseContext,
                            BibEntry entry,
                            AiService aiService,
                            AiPreferences aiPreferences,
                            ExternalApplicationsPreferences externalApplicationsPreferences,
                            CitationKeyPatternPreferences citationKeyPatternPreferences,
                            DialogService dialogService,
                            AdaptVisibleTabs adaptVisibleTabs
    ) {
        super(aiPreferences, externalApplicationsPreferences, dialogService, adaptVisibleTabs);

        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences);
        this.aiService = aiService;
        this.aiPreferences = aiPreferences;
        this.dialogService = dialogService;

        aiService.getSummariesService().summarize(entry, bibDatabaseContext).stateProperty().addListener(o -> rebuildUi());

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
        } else if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry)) {
            return tryToGenerateCitationKeyThenBind(entry);
        } else {
            return tryToShowSummary();
        }
    }

    private Node showErrorNoDatabasePath() {
        return new ErrorStateComponent(
                Localization.lang("Unable to generate summary"),
                Localization.lang("The path of the current library is not set, but it is required for summarization")
        );
    }

    private Node showErrorNotPdfs() {
        return new ErrorStateComponent(
                Localization.lang("Unable to generate summary"),
                Localization.lang("Only PDF files are supported.")
        );
    }

    private Node showErrorNoFiles() {
        return new ErrorStateComponent(
                Localization.lang("Unable to generate summary"),
                Localization.lang("Please attach at least one PDF file to enable summarization of PDF file(s).")
        );
    }

    private Node tryToGenerateCitationKeyThenBind(BibEntry entry) {
        if (citationKeyGenerator.generateAndSetKey(entry).isEmpty()) {
            return new ErrorStateComponent(
                    Localization.lang("Unable to generate summary"),
                    Localization.lang("Please provide a non-empty and unique citation key for this entry.")
            );
        } else {
            return showPrivacyPolicyGuardedContent();
        }
    }

    private Node tryToShowSummary() {
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
                Localization.lang("The attached file(s) are currently being processed by %0. Once completed, you will be able to see the summary.", aiPreferences.getSelectedChatModel())
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
        },
        () ->exportMarkdown(summary),
        () ->exportJson(summary));
    }

    private void exportJson(Summary summary) {
        if (summary == null) {
            dialogService.notify(Localization.lang("No summary available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.JSON)
                .withDefaultExtension(StandardFileType.JSON)
                .withInitialDirectory(Path.of(System.getProperty("user.home")))
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                .ifPresent(path -> {
                    try {
                        Map<String, Object> root = new HashMap<>();
                        root.put("latest_provider", summary.aiProvider().getLabel());
                        root.put("latest_model", summary.model());
                        root.put("timestamp", summary.timestamp().toString());

                        Map<String, String> entryMap = new HashMap<>();
                        for (Field field : entry.getFields()) {
                            entryMap.put(field.getName(), entry.getField(field).orElse(""));
                        }
                        root.put("entry", entryMap);


                        StringBuilder bibtex = new StringBuilder();
                        bibtex.append("@").append(entry.getType().getName()).append("{").append(entry.getCitationKey().orElse("")).append(",\n");
                        for (Field field : entry.getFields()) {
                            bibtex.append("  ").append(field.getName()).append(" = {").append(entry.getField(field).orElse("")).append("},\n");
                        }
                        bibtex.append("}");
                        root.put("entry_bibtex", bibtex.toString());


                        List<Map<String, String>> conversation = new ArrayList<>();
                        Map<String, String> message = new HashMap<>();
                        message.put("role", "assistant");
                        message.put("content", summary.content());
                        conversation.add(message);
                        root.put("conversation", conversation);


                        ObjectMapper mapper = new ObjectMapper();
                        mapper.enable(SerializationFeature.INDENT_OUTPUT);

                        String jsonString = mapper.writeValueAsString(root);

                        Files.writeString(path, jsonString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        dialogService.notify(Localization.lang("Export successful"));

                    } catch (java.io.IOException e) {
                        LOGGER.error("Problem occurred while writing the export file", e);
                        dialogService.showErrorDialogAndWait(Localization.lang("Save failed"), e);
                    }
                });
    }
    private void exportMarkdown(Summary summary) {
        if (summary == null) {
            dialogService.notify(Localization.lang("No summary available to export"));
            return;
        }
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.MARKDOWN)
                .withDefaultExtension(StandardFileType.MARKDOWN)
                .withInitialDirectory(Path.of(System.getProperty("user.home")))
                .build();

        StringBuilder sb = new StringBuilder();
        sb.append("## Bibtex\n\n```bibtex\n");
        sb.append("@").append(entry.getType().getName()).append("{").append(entry.getCitationKey().orElse("")).append(",\n");

        for (Field field : entry.getFields()) {
            String value = entry.getField(field).orElse("");
            sb.append("  ").append(field.getName()).append(" = {").append(value).append("},\n");
        }
        sb.append("}\n```\n\n");
        sb.append("## Summary\n\n");
        sb.append(summary.content());

        String finalContent = sb.toString();
        dialogService.showFileSaveDialog(fileDialogConfiguration)
                .ifPresent(path -> {
                    try {
                        Files.writeString(path, finalContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        dialogService.notify("Export successful");
                    } catch (Exception e) {
                        LOGGER.error("Problem occurred while writing the export file", e);
                        dialogService.showErrorDialogAndWait(Localization.lang("Save failed"), e);
                    }
                });
    }
}
