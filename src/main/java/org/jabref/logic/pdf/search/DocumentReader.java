package org.jabref.logic.pdf.search;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log LOGGER = LogFactory.getLog(DocumentReader.class);

    /**
     * Reads the content and metadata from a pdf file
     */
    public Document readPDFContents(File pdf, String bibTexKey) {

        Document newDocument = new Document();

        try {
            PDDocument pdfDocument = PDDocument.load(pdf);
            PDDocumentInformation info = pdfDocument.getDocumentInformation();

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setLineSeparator("\n");
            newDocument.add(normalizeField(KEY, bibTexKey));
            newDocument.add(normalizeField(CONTENT, pdfTextStripper.getText(pdfDocument)));
            newDocument.add(normalizeField(AUTHOR, info.getAuthor()));
            newDocument.add(normalizeField(CREATOR, info.getCreator()));
            newDocument.add(normalizeField(SUBJECT, info.getSubject()));
            newDocument.add(normalizeField(KEYWORDS, info.getKeywords()));
            pdfDocument.close();
        } catch (IOException e) {
            LOGGER.debug("Could not read pdf file: " + pdf.toString() + "!", e);
        }

        return newDocument;
    }

    /**
     * Safely add a field to a document so that null values are not indexed and added as empty strings to prevent a
     * NullPointerException
     *
     * @param field the field name
     * @param value the value to add to the field, gets mapped to a empty string if null
     */
    private Field normalizeField(String field, String value) {
        if (value == null) {
            return new Field(field, "", Field.Store.YES, Field.Index.NO);
        }

        return new Field(field, value, Field.Store.YES, Field.Index.ANALYZED);
    }
}
