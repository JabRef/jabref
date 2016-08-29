package net.sf.jabref.logic.pdf;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;

public class PdfCommentImporter {

    private List pdfPages;
    private PDPage page;
    public PdfCommentImporter() {

    }

    /**
     * Imports the comments from a pdf specified by its URI
     * @param pathToPDF the URI specifying the document
     * @return a hasmap with the unique name as key and the notes content as value
     */
    public HashMap<String, String> importNotes(final URI pathToPDF){

        PDDocument pdf;
        HashMap<String, String> annotationsMap = new HashMap<>();
        try {
            pdf = importPdfFile(pathToPDF);

            pdfPages = pdf.getDocumentCatalog().getAllPages();
            for(int i = 0; i < pdfPages.size(); i++){
                page = (PDPage) pdfPages.get(i);
                for(PDAnnotation annotation : page.getAnnotations()){
                    annotationsMap.put(annotation.getAnnotationName(), annotation.getContents());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return annotationsMap;
    }

    private PDDocument importPdfFile(final URI pathToPDF) throws IOException {
        File file = new File(pathToPDF);
        return PDDocument.load(file);
    }
}
