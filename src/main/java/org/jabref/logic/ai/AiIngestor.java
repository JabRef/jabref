package org.jabref.logic.ai;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an "algorithm class". Meaning it is used in one place and is thrown away quickly.
 * <p>
 * This class contains a bunch of methods that are useful for loading the documents to AI.
 */
public class AiIngestor {
    // Another "algorithm class" that ingests the contents of the file into the embedding store.
    private final EmbeddingStoreIngestor ingestor;

    private static final Logger LOGGER = LoggerFactory.getLogger(AiIngestor.class.getName());

    public AiIngestor(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        // TODO: Tweak the parameters of this object.
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(300, 0);

        this.ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel) // What if null?
                .documentSplitter(documentSplitter)
                .build();
    }

    public void ingestString(String contents) {
        LOGGER.trace("Ingesting: {}", contents);
        Document document = new Document(contents);
        ingestor.ingest(document);
    }

    public void ingestPDFFile(Path path) throws IOException {
        PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(path);
        PDFTextStripper stripper = new PDFTextStripper();

        int lastPage = document.getNumberOfPages();
        stripper.setStartPage(1);
        stripper.setEndPage(lastPage);
        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);

        String result = writer.toString();

        ingestString(result);
    }

    public void ingestLinkedFile(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences) throws IOException, JabRefException {
        if (!"PDF".equals(linkedFile.getFileType())) {
            String errorMsg = Localization.lang("Unsupported file type") + ": "
                    + linkedFile.getFileType() + ". "
                    + Localization.lang("Only PDF files are supported") + ".";
            throw new JabRefException(errorMsg);
        }

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);
        if (path.isPresent()) {
            ingestPDFFile(path.get());
        } else {
            throw new FileNotFoundException(linkedFile.getLink());
        }
    }
}
