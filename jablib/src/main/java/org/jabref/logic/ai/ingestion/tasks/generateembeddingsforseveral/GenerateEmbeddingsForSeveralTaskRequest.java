package org.jabref.logic.ai.ingestion.tasks.generateembeddingsforseveral;

import java.util.List;

import javafx.beans.property.StringProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public record GenerateEmbeddingsForSeveralTaskRequest(
        FilePreferences filePreferences,
        IngestedDocumentsRepository ingestedDocumentsRepository,
        EmbeddingStore<TextSegment> embeddingStore,
        EmbeddingModel embeddingModel,
        DocumentSplitter documentSplitter,
        BibDatabaseContext bibDatabaseContext,
        StringProperty groupName,
        List<LinkedFile> linkedFiles,
        TaskExecutor taskExecutor
) {
}
