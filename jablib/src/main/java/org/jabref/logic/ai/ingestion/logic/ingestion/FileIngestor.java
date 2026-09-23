package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.l10n.Localization;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class FileIngestor {
    public static final String PAGE_NUMBER_METADATA_KEY = "pageNumber";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileIngestor.class);

    private final UniversalContentParser universalFileParser;
    private final TextIngestor textIngestor;

    public FileIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this(
                new UniversalContentParser(),
                new TextIngestor(
                        embeddingStore,
                        embeddingModel,
                        documentSplitter
                )
        );
    }

    FileIngestor(UniversalContentParser universalFileParser, TextIngestor textIngestor) {
        this.universalFileParser = universalFileParser;
        this.textIngestor = textIngestor;
    }

    public void ingest(Metadata metadata, Path path) throws InterruptedException {
        List<String> pages = universalFileParser.parse(path);

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        if (pages.isEmpty() || pages.stream().allMatch(String::isBlank)) {
            LOGGER.error("Unable to generate embeddings for file \"{}\", because JabRef was unable to extract text from the file", path);
            throw new RuntimeException(Localization.lang("Unable to generate embeddings for file '%0', because JabRef was unable to extract text from the file", path.toString()));
        }

        int pageNumber = 1;
        for (String pageText : pages) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (!pageText.isBlank()) {
                Metadata pageMetadata = metadata.copy();
                pageMetadata.put(PAGE_NUMBER_METADATA_KEY, pageNumber);

                textIngestor.ingest(
                        pageMetadata,
                        pageText
                );
            }
            pageNumber++;
        }

        LOGGER.debug("Embeddings for file \"{}\" were generated successfully", path);
    }
}
