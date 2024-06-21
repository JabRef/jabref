package org.jabref.logic.ai.embeddings;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main logic class that is responsible for generating embeddings out of {@link LinkedFile}s.
 */
public class AiIngestor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiIngestor.class.getName());

    private final AiService aiService;

    private EmbeddingStoreIngestor ingestor;

    public AiIngestor(AiService aiService) {
        this.aiService = aiService;
        this.ingestor = rebuild(aiService);

        listenToPreferences();
    }

    private EmbeddingStoreIngestor rebuild(AiService aiService) {
        DocumentSplitter documentSplitter = DocumentSplitters
                .recursive(aiService.getPreferences().getDocumentSplitterChunkSize(),
                           aiService.getPreferences().getDocumentSplitterOverlapSize());

        return EmbeddingStoreIngestor
                .builder()
                .embeddingStore(aiService.getEmbeddingsManager().getEmbeddingsStore())
                .embeddingModel(aiService.getEmbeddingModel().getEmbeddingModel())
                .documentSplitter(documentSplitter)
                .build();
    }

    private void listenToPreferences() {
        aiService.getPreferences().onEmbeddingsParametersChange(() -> ingestor = rebuild(aiService));
    }

    /**
     * The main method for generating embeddings out of {@link LinkedFile}s.
     * The method will check if the file was ingested. In case it is, it will do nothing.
     * The embeddings will be generated if the file was not ingested yet or the file was modified.
     */
    public void ingestLinkedFile(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences) {
        AiIngestedFilesTracker ingestedFilesTracker = aiService.getEmbeddingsManager().getIngestedFilesTracker();

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file: {}", linkedFile.getLink());
            return;
        }

        try {
            BasicFileAttributes attributes = Files.readAttributes(path.get(), BasicFileAttributes.class);

            long currentModificationTimeInSeconds = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

            Long ingestedModificationTimeInSeconds = ingestedFilesTracker.getIngestedFileModificationTime(linkedFile.getLink());

            if (ingestedModificationTimeInSeconds != null && currentModificationTimeInSeconds <= ingestedModificationTimeInSeconds) {
                return;
            }

            ingestFile(path.get(), new Metadata().put("linkedFile", linkedFile.getLink()));
            ingestedFilesTracker.endIngestingFile(linkedFile.getLink(), attributes.lastModifiedTime().to(TimeUnit.SECONDS));
        } catch (IOException e) {
            LOGGER.error("Couldn't retrieve attributes of a linked file: {}", linkedFile.getLink(), e);
            LOGGER.warn("Regenerating embeddings for linked file: {}", linkedFile.getLink());

            ingestFile(path.get(), new Metadata().put("linkedFile", linkedFile.getLink()));
            ingestedFilesTracker.endIngestingFile(linkedFile.getLink(), 0);
        }
    }

    private void ingestFile(Path path, Metadata metadata) {
        if (FileUtil.isPDFFile(path)) {
            ingestPDFFile(path, metadata);
        } else {
            LOGGER.info("Unsupported file type of file: {}. For now, only PDF files are supported", path);
        }
    }

    private void ingestPDFFile(Path path, Metadata metadata) {
        try {
            PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(path);
            PDFTextStripper stripper = new PDFTextStripper();

            int lastPage = document.getNumberOfPages();
            stripper.setStartPage(1);
            stripper.setEndPage(lastPage);
            StringWriter writer = new StringWriter();
            stripper.writeText(document, writer);

            ingestString(writer.toString(), metadata);
        } catch (Exception e) {
            LOGGER.error("An error occurred while reading a PDF file: {}", path, e);
        }
    }

    private void ingestString(String string, Metadata metadata) {
        ingestDocument(new Document(string, metadata));
    }

    private void ingestDocument(Document document) {
        LOGGER.trace("Ingesting: {}", document);
        ingestor.ingest(document);
    }
}
