package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

import static org.jabref.logic.pdf.search.SearchFieldConstants.AUTHOR;
import static org.jabref.logic.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.logic.pdf.search.SearchFieldConstants.CREATOR;
import static org.jabref.logic.pdf.search.SearchFieldConstants.KEY;
import static org.jabref.logic.pdf.search.SearchFieldConstants.KEYWORDS;
import static org.jabref.logic.pdf.search.SearchFieldConstants.SUBJECT;

public class DocumentReader {

    private final BibEntry entry;

    public DocumentReader(BibEntry bibEntry) {
        if (!bibEntry.getField(FieldName.FILE).isPresent()) {
            throw new IllegalArgumentException("The file field must not be absent when trying to reading the " +
                    "document!");
        }

        this.entry = bibEntry;
    }

    /**
     * Reads the content and metadata from a pdf file
     */
    public Document readPDFContents() throws IOException {
        Path pdfPath = Paths.get(this.entry.getField(FieldName.FILE).get());

        try (PDDocument pdfDocument = PDDocument.load(pdfPath.toFile())) {
            PDDocumentInformation info = pdfDocument.getDocumentInformation();

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setLineSeparator("\n");

            Document newDocument = new Document();
            if (this.entry.getCiteKeyOptional().isPresent()) {
                newDocument.add(normalizeField(KEY, this.entry.getCiteKeyOptional().get()));
            } else {
                newDocument.add(normalizeField(KEY, ""));
            }
            newDocument.add(normalizeField(CONTENT, pdfTextStripper.getText(pdfDocument)));
            newDocument.add(normalizeField(AUTHOR, info.getAuthor()));
            newDocument.add(normalizeField(CREATOR, info.getCreator()));
            newDocument.add(normalizeField(SUBJECT, info.getSubject()));
            newDocument.add(normalizeField(KEYWORDS, info.getKeywords()));
            return newDocument;
        } catch (IOException e) {
            throw new IOException("Could not read pdf file: " + pdfPath + "!", e);
        }
    }

    /**
     * Safely add a field to a document so that null values are not indexed and added as empty strings to prevent a
     * NullPointerException
     *
     * @param field the field name
     * @param value the value to add to the field, gets mapped to a empty string if null
     */
    private Field normalizeField(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            return new Field(field, "", Field.Store.YES, Field.Index.NO);
        }

        return new Field(field, value, Field.Store.YES, Field.Index.ANALYZED);
    }
}
