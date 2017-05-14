package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

import static org.jabref.model.pdf.search.SearchFieldConstants.AUTHOR;
import static org.jabref.model.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.model.pdf.search.SearchFieldConstants.CREATOR;
import static org.jabref.model.pdf.search.SearchFieldConstants.KEY;
import static org.jabref.model.pdf.search.SearchFieldConstants.KEYWORDS;
import static org.jabref.model.pdf.search.SearchFieldConstants.SUBJECT;

/**
 * Utility class for reading the data from LinkedFiles of a BibEntry for Lucene.
 */
public final class DocumentReader {
    private static final Log LOGGER = LogFactory.getLog(DocumentReader.class);

    private final BibEntry entry;

    /**
     * Creates a new DocumentReader using a BibEntry.
     *
     * @param bibEntry Must not be null and must have at least one LinkedFile.
     */
    public DocumentReader(BibEntry bibEntry) {
        if (bibEntry.getFiles().isEmpty()) {
            throw new IllegalStateException("There are no linked PDF files to this BibEntry!");
        }

        this.entry = bibEntry;
    }

    /**
     * Reads each LinkedFile of a BibEntry and converts them into Lucene Documents which are then returned.
     *
     * @return A List of Documents with the (meta)data. Can be empty if there is a problem reading the LinkedFile.
     */
    public List<Document> readLinkedPdfs() {
        List<Document> documents = new LinkedList<>();
        for (LinkedFile pdf : this.entry.getFiles()) {
            try {
                documents.add(readPdfContents(Paths.get(pdf.getLink())));
            } catch (IOException ioe) {
                LOGGER.info("Could not read pdf file: " + pdf.getLink() + "!", ioe);
            }
        }
        return documents;
    }

    private Document readPdfContents(Path pdfPath) throws IOException {
        try (PDDocument pdfDocument = PDDocument.load(pdfPath.toFile())) {
            Document newDocument = new Document();
            addKeyIfPresent(newDocument);
            addContentIfNotEmpty(pdfDocument, newDocument);
            addMetaData(pdfDocument, newDocument);
            return newDocument;
        }
    }

    private void addMetaData(PDDocument pdfDocument, Document newDocument) {
        PDDocumentInformation info = pdfDocument.getDocumentInformation();
        addStringField(newDocument, AUTHOR, info.getAuthor());
        addStringField(newDocument, CREATOR, info.getCreator());
        addStringField(newDocument, SUBJECT, info.getSubject());
        addTextField(newDocument, KEYWORDS, info.getKeywords());
    }

    private void addTextField(Document newDocument, String field, String value) {
        if (!isValidField(value)) {
            return;
        }
        newDocument.add(new TextField(field, value, Field.Store.YES));
    }

    private void addStringField(Document newDocument, String field, String value) {
        if (!isValidField(value)) {
            return;
        }
        newDocument.add(new StringField(field, value, Field.Store.YES));
    }

    private boolean isValidField(String value) {
        return !(StringUtil.isNullOrEmpty(value));
    }

    private void addContentIfNotEmpty(PDDocument pdfDocument, Document newDocument) {
        try {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setLineSeparator("\n");

            String pdfContent = pdfTextStripper.getText(pdfDocument);
            if (StringUtil.isNotBlank(pdfContent)) {
                newDocument.add(new TextField(CONTENT, pdfContent, Field.Store.YES));
            }
        } catch (IOException e) {
            LOGGER.info("Could not read contents of PDF document " + pdfDocument.toString(), e);
        }
    }

    private void addKeyIfPresent(Document newDocument) {
        if (this.entry.getCiteKeyOptional().isPresent()) {
            newDocument.add(new StringField(KEY, this.entry.getCiteKeyOptional().get(), Field.Store.YES));
        }
    }
}
