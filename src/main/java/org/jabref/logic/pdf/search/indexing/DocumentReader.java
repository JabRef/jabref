package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

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

public final class DocumentReader {

    private final BibEntry entry;
    private final PDFTextStripper pdfTextStripper = new PDFTextStripper();

    public DocumentReader(BibEntry bibEntry) throws IOException {
        if (!bibEntry.getField(FieldName.FILE).isPresent()) {
            throw new IllegalArgumentException("The file field must not be absent when trying to reading the " +
                    "document!");
        }
        this.entry = bibEntry;

        pdfTextStripper.setLineSeparator("\n");
    }

    /**
     * Reads the content and metadata from a pdf file
     */
    public Document readPDFContents() throws IOException {
        Path pdfPath = Paths.get(this.entry.getField(FieldName.FILE).get());

        try (PDDocument pdfDocument = PDDocument.load(pdfPath.toFile())) {
            Document newDocument = new Document();
            addKeyIfPresent(newDocument);
            addContentIfNotEmpty(pdfDocument, newDocument);
            addMetaData(pdfDocument, newDocument);
            return newDocument;
        } catch (IOException e) {
            throw new IOException("Could not read pdf file: " + pdfPath + "!", e);
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
        return !(value == null || value.trim().isEmpty());
    }

    private void addContentIfNotEmpty(PDDocument pdfDocument, Document newDocument) throws IOException {
        String pdfContent = pdfTextStripper.getText(pdfDocument);
        if (!pdfContent.trim().isEmpty()) {
            newDocument.add(new TextField(CONTENT, pdfContent, Field.Store.YES));
        }
    }

    private void addKeyIfPresent(Document newDocument) {
        if (this.entry.getCiteKeyOptional().isPresent()) {
            newDocument.add(new StringField(KEY, this.entry.getCiteKeyOptional().get(), Field.Store.YES));
        }
    }
}
