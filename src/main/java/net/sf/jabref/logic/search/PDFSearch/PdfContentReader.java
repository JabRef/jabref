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

    public static Document readContentFromPDFToString(File documentFile) {

        Document content = new Document();

        try {
            PDDocument document = PDDocument.load(documentFile);
            PDDocumentInformation info = document.getDocumentInformation();

            PDFTextStripper candy = new PDFTextStripper();
            candy.setLineSeparator("\n");
            content.add(new Field("content", candy.getText(document), Field.Store.YES,
                    Field.Index.ANALYZED));
            document.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }
}
