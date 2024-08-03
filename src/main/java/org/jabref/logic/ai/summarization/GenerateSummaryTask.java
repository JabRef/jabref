package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.embeddings.FileToDocument;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSummaryTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryTask.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final String citationKey;
    private final List<LinkedFile> linkedFiles;
    private final AiService aiService;
    private final FilePreferences filePreferences;

    public GenerateSummaryTask(BibDatabaseContext bibDatabaseContext,
                               String citationKey,
                               List<LinkedFile> linkedFiles,
                               AiService aiService,
                               FilePreferences filePreferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.citationKey = citationKey;
        this.linkedFiles = linkedFiles;
        this.aiService = aiService;
        this.filePreferences = filePreferences;

        titleProperty().set(Localization.lang("Generating summary for for %0", citationKey));
    }

    @Override
    protected Void call() throws Exception {
        showToUser(true);

        try {
            summarizeAll();
        } catch (InterruptedException e) {
            LOGGER.info("There was a summarization task for {}. It will be canceled, because user quits JabRef", citationKey);
        }

        showToUser(false);

        return null;
    }

    private void summarizeAll() throws InterruptedException {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.error("No summary can be generated for entry as the database doesn't have path");
            return;
        }

        // Stream API would look better here, but we need to catch InterruptedException.
        List<String> linkedFilesSummary = new ArrayList<>();
        for (LinkedFile linkedFile : linkedFiles) {
            Optional<String> s = generateSummary(linkedFile);
            if (s.isPresent()) {
                String string = s.get();
                linkedFilesSummary.add(string);
            }
        }

        if (linkedFilesSummary.isEmpty()) {
            LOGGER.error("No summary can be generated for entry");
            return;
        }

        String finalSummary;

        if (linkedFilesSummary.size() == 1) {
            finalSummary = linkedFilesSummary.getFirst();
        } else {
            finalSummary = SummarizationAlgorithm.summarize(aiService.getShutdownSignal(), aiService.getChatLanguageModel(), aiService.getPreferences().getContextWindowSize(), linkedFilesSummary.stream());
        }

        aiService.getSummariesStorage().set(bibDatabaseContext.getDatabasePath().get(), citationKey, finalSummary);
    }

    private Optional<String> generateSummary(LinkedFile linkedFile) throws InterruptedException {
        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file: {}", linkedFile.getLink());
            return Optional.empty();
        }

        Optional<Document> document = FileToDocument.fromFile(path.get());

        if (document.isEmpty()) {
            LOGGER.warn("Generated empty document from a linked file {}. It will be skipped when generating a summary", linkedFile.getLink());
            return Optional.empty();
        }

        String linkedFileSummary = SummarizationAlgorithm.summarize(aiService.getShutdownSignal(), aiService.getChatLanguageModel(), aiService.getPreferences().getContextWindowSize(), document.get().text());

        return Optional.of(linkedFileSummary);
    }
}
