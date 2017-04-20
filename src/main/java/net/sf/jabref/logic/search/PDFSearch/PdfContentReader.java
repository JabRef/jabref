package net.sf.jabref.logic.search.PDFSearch;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * Created by christoph on 02.08.16.
 */
public class PdfContentReader {

    /**
     * Reads the content and metadata from a pdf file
     *
     * @param documentFile
     * @param bibTexKey
     * @return
     */
    public Document readContentFromPDFToString(File documentFile, String bibTexKey) {

        Document content = new Document();

        try {
            PDDocument document = PDDocument.load(documentFile);
            PDDocumentInformation info = document.getDocumentInformation();

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setLineSeparator("\n");
            securelyAddFieldToDocument(content, SearchFieldConstants.KEY, bibTexKey);
            securelyAddFieldToDocument(content, SearchFieldConstants.CONTENT, pdfTextStripper.getText(document));
            securelyAddFieldToDocument(content, SearchFieldConstants.AUTHOR, info.getAuthor());
            securelyAddFieldToDocument(content, SearchFieldConstants.CREATOR, info.getCreator());
            securelyAddFieldToDocument(content, SearchFieldConstants.SUBJECT, info.getSubject());
            securelyAddFieldToDocument(content, SearchFieldConstants.KEYWORDS, info.getKeywords());
            document.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

    /**
     * Safely add a field to a document so that null values are not indexed and added as empty strings to prevent a
     * NullPointerException
     *
     * @param document document to add the field to
     * @param field    the field name
     * @param value    the value to add to the field, gets mapped to a empty string if null
     */
    private void securelyAddFieldToDocument(Document document, String field, String value) {
        if (value == null) {
            document.add(new Field(field, "", Field.Store.YES, Field.Index.NO));
        } else {
            document.add(new Field(field, value, Field.Store.YES, Field.Index.ANALYZED));
        }
    }
}
