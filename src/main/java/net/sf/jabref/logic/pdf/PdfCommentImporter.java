package net.sf.jabref.logic.pdf;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.pdf.PdfComment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.fdf.FDFAnnotationHighlight;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.util.PDFTextStripperByArea;

public class PdfCommentImporter {

    private final int APPREVIATED_ANNOTATIONANE_LENGTH = 20;
    private List pdfPages;
    private PDPage page;
    private Log logger = LogFactory.getLog(PdfCommentImporter.class);

    public PdfCommentImporter() {

    }

    /**
     * Imports the comments from a pdf specified by its URI
     *
     * @param document a PDDocument to get the annotations from
     * @return a hashmap with the unique name as key and the notes content as value
     */
    public HashMap<String, PdfComment> importNotes(final PDDocument document) {

        HashMap<String, PdfComment> annotationsMap = new HashMap<>();

        pdfPages = document.getDocumentCatalog().getAllPages();
        for (int i = 0; i < pdfPages.size(); i++) {
            page = (PDPage) pdfPages.get(i);
            try {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation.getSubtype().equals(FDFAnnotationHighlight.SUBTYPE)) {
                        PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();

                        COSArray quadsArray = (COSArray) annotation.getDictionary().getDictionaryObject(COSName.getPDFName("QuadPoints"));
                        String highlightedText = null;
                        for (int j = 1, k = 0; j <= (quadsArray.size() / 8); j++) {

                            COSFloat upperLeftX = (COSFloat) quadsArray.get(0 + k);
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
                        int appreviatedContentLengthForName = APPREVIATED_ANNOTATIONANE_LENGTH;
                        if(highlightedText.length() < appreviatedContentLengthForName){
                            appreviatedContentLengthForName = highlightedText.length();
                        }
                        annotationsMap.put(highlightedText.subSequence(0, appreviatedContentLengthForName).toString() + "...", new PdfComment(annotation, i));
                    } else {
                        annotationsMap.put(annotation.getAnnotationName(), new PdfComment(annotation, i));
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
        return annotationsMap;
    }

    public List<PDDocument> importPdfFile(final List<BibEntry> entryList, final BibDatabaseContext bibDatabaseContext) throws IOException {

        final List<File> files = FileUtil.getListOfLinkedFiles(entryList,
                bibDatabaseContext.getFileDirectory(Globals.prefs.getFileDirectoryPreferences()));

        ArrayList<PDDocument> documents = new ArrayList<>();
        for(File linkedFile : files){
            if(linkedFile.getName().toLowerCase().endsWith(".pdf")){
                documents.add(PDDocument.load(linkedFile));
            }
        }
        return documents;
    }
}
