package net.sf.jabref.logic.pdf;

import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.pdf.PdfComment;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.fdf.FDFAnnotationHighlight;
import org.apache.pdfbox.pdmodel.fdf.FDFAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.util.PDFTextStripperByArea;

public class PdfCommentImporter {

    private List pdfPages;
    private PDPage page;

    public PdfCommentImporter() {

    }

    /**
     * Imports the comments from a pdf specified by its path
     *
     * @param path a path to a pdf
     * @return a list with the all the annotations found in the file of the path
     */
    public List<PdfComment> importNotes(final String path, final BibDatabaseContext context) {

        List<PdfComment> annotationsList = new ArrayList<>();

        PDDocument document = null;
        try {
            try{
                document = importPdfFile(path);
            } catch (FileNotFoundException notFound) {
                String absolutePath = FileUtil.expandFilename(context, path,
                        JabRefPreferences.getInstance().getFileDirectoryPreferences())
                        .get().getAbsolutePath();
                document = importPdfFile(absolutePath);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        pdfPages = document.getDocumentCatalog().getAllPages();
        for (int i = 0; i < pdfPages.size(); i++) {
            page = (PDPage) pdfPages.get(i);
            try {
                for (PDAnnotation annotation : page.getAnnotations()) {

                    String subtype = annotation.getSubtype();
                    //highlighted text has to be extracted by the rectangle calculated from the highlighting
                    if (subtype.equals(FDFAnnotationHighlight.SUBTYPE)) {

                        PdfComment annotationBelongingToHighlighting = new PdfComment(annotation.getAnnotationName(),
                                annotation.getDictionary().getString(COSName.T), annotation.getModifiedDate(),
                                i + 1, annotation.getContents(), FDFAnnotationText.SUBTYPE);

                        PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();
                        COSArray quadsArray = (COSArray) annotation.getDictionary().getDictionaryObject(COSName.getPDFName("QuadPoints"));
                        String highlightedText = null;
                        for (int j = 1, k = 0; j <= (quadsArray.size() / 8); j++) {

                            COSFloat upperLeftX = (COSFloat) quadsArray.get(k);
                            COSFloat upperLeftY = (COSFloat) quadsArray.get(1 + k);
                            COSFloat upperRightX = (COSFloat) quadsArray.get(2 + k);
                            COSFloat upperRightY = (COSFloat) quadsArray.get(3 + k);
                            COSFloat lowerLeftX = (COSFloat) quadsArray.get(4 + k);
                            COSFloat lowerLeftY = (COSFloat) quadsArray.get(5 + k);

                            k += 8;

                            float ulx = upperLeftX.floatValue() - 1;
                            float uly = upperLeftY.floatValue();
                            float width = upperRightX.floatValue() - lowerLeftX.floatValue();
                            float height = upperRightY.floatValue() - lowerLeftY.floatValue();

                            PDRectangle pageSize = page.getMediaBox();
                            uly = pageSize.getHeight() - uly;

                            Rectangle2D.Float rectangle = new Rectangle2D.Float(ulx, uly, width, height);
                            stripperByArea.addRegion("highlightedRegion", rectangle);
                            stripperByArea.extractRegions(page);
                            String highlightedTextInLine = stripperByArea.getTextForRegion("highlightedRegion");

                            if (j > 1) {
                                highlightedText = highlightedText.concat(highlightedTextInLine);
                            } else {
                                highlightedText = highlightedTextInLine;
                            }
                        }
                        annotation.setContents(highlightedText);

                        PdfComment highlighting = new PdfComment(annotation, i + 1);
                        //highlighted text that has a sticky note on it should be linked to the sticky note
                        highlighting.linkComments(annotationBelongingToHighlighting);
                        annotationsList.add(annotationBelongingToHighlighting);
                        annotationsList.add(highlighting);

                    } else {
                        annotationsList.add(new PdfComment(annotation, i + 1));
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        try {
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return annotationsList;
    }

    /**
     *
     * @param path the absolute path to the pdf file
     * @return a PDDocument representing the pdf file
     * @throws IOException
     */
    public PDDocument importPdfFile(final String path) throws IOException {

            if(path.toLowerCase().endsWith(".pdf")){
               return PDDocument.load("/"+ path);
            }
        return null;
    }
}
