package org.jabref.logic.ai.summarization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.FileEmbeddingsManager;
import org.jabref.logic.ai.embeddings.FileToDocument;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.ws.rs.core.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSummaryTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryTask.class);

    private final BibDatabaseContext bibDatabaseContext;
    private final String citationKey;
    private final List<LinkedFile> linkedFiles;
    private final ChatLanguageModel chatLanguageModel;
    private final SummariesStorage summariesStorage;
    private final FilePreferences filePreferences;

    public GenerateSummaryTask(BibDatabaseContext bibDatabaseContext,
                               String citationKey,
                               List<LinkedFile> linkedFiles,
                               ChatLanguageModel chatLanguageModel,
                               SummariesStorage summariesStorage,
                               FilePreferences filePreferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.citationKey = citationKey;
        this.linkedFiles = linkedFiles;
        this.chatLanguageModel = chatLanguageModel;
        this.summariesStorage = summariesStorage;
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

        String finalSummary = SummarizationAlgorithm.summarize(chatLanguageModel, linkedFilesSummary.stream());

        summariesStorage.set(bibDatabaseContext.getDatabasePath().get(), citationKey, finalSummary);

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

        String linkedFileSummary = SummarizationAlgorithm.summarize(chatLanguageModel, document.get().text());

        return Optional.of(linkedFileSummary);
    }
}
