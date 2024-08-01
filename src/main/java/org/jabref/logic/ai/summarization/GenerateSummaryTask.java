package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.embeddings.FileToDocument;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
    }

    @Override
    protected Void call() throws Exception {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.error("No summary can be generated for entry as the database doesn't have path");
            return null;
        }

        List<String> linkedFilesSummary = linkedFiles
                .stream()
                .map(this::generateSummary)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (linkedFilesSummary.isEmpty()) {
            LOGGER.error("No summary can be generated for entry");
            return null;
        }

        String finalSummary;

        if (linkedFilesSummary.size() == 1) {
            finalSummary = linkedFilesSummary.getFirst();
        } else {
            finalSummary = SummarizationAlgorithm.summarize(aiService.getChatLanguageModel(), aiService.getPreferences().getContextWindowSize(), linkedFilesSummary.stream());
        }

        aiService.getSummariesStorage().set(bibDatabaseContext.getDatabasePath().get(), citationKey, finalSummary);

        return null;
    }

    private Optional<String> generateSummary(LinkedFile linkedFile) {
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

        String linkedFileSummary = SummarizationAlgorithm.summarize(aiService.getChatLanguageModel(), aiService.getPreferences().getContextWindowSize(), document.get().text());

        return Optional.of(linkedFileSummary);
    }
}
