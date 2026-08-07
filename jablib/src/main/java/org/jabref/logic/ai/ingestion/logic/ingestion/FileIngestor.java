package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.l10n.Localization;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileIngestor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileIngestor.class);

    private final UniversalContentParser universalFileParser;
    private final TextIngestor textIngestor;

    public FileIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.universalFileParser = new UniversalContentParser();
        this.textIngestor = new TextIngestor(
                embeddingStore,
                embeddingModel,
                documentSplitter
        );
    }

    public void ingest(Metadata metadata, Path path) throws InterruptedException {
        Optional<String> document = universalFileParser.parse(path);

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        if (document.isPresent()) {
            textIngestor.ingest(
                    metadata,
                    document.get()
            );
            LOGGER.debug("Embeddings for file \"{}\" were generated successfully", path.toString());
        } else {
            LOGGER.error("Unable to generate embeddings for file \"{}\", because JabRef was unable to extract text from the file", path.toString());
            throw new RuntimeException(Localization.lang("Unable to generate embeddings for file '%0', because JabRef was unable to extract text from the file", path.toString()));
        }
    }
}
