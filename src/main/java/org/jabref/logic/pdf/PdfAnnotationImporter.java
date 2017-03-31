package org.jabref.logic.pdf;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.util.PDFTextStripperByArea;


public class PdfAnnotationImporter implements AnnotationImporter {

    private static final Log LOGGER = LogFactory.getLog(PdfAnnotationImporter.class);

    /**
     * Imports the comments from a pdf specified by its path
     *
     * @param path a path to a pdf
     * @return a list with the all the annotations found in the file of the path
     */
    @Override
    public List<FileAnnotation> importAnnotations(final Path path) {

        if (!validatePath(path)) {
            // Path could not be validated, return default result
            return Collections.emptyList();
        }

        List<FileAnnotation> annotationsList = new LinkedList<>();
        try (PDDocument document = PDDocument.load(path.toString())) {
            List pdfPages = document.getDocumentCatalog().getAllPages();
            for (int pageIndex = 0; pageIndex < pdfPages.size(); pageIndex++) {
                PDPage page = (PDPage) pdfPages.get(pageIndex);
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (!isSupportedAnnotationType(annotation)) {
                        continue;
                    }
                    if (FileAnnotationType.UNDERLINE.toString().equals(annotation.getSubtype()) ||
                            FileAnnotationType.HIGHLIGHT.toString().equals(annotation.getSubtype())) {
                        annotationsList.add(createMarkedAnnotations(pageIndex, page, annotation));
                    } else {
                        FileAnnotation fileAnnotation = new FileAnnotation(annotation, pageIndex + 1);
                        if (fileAnnotation.getContent() != null && !fileAnnotation.getContent().isEmpty()) {
                            annotationsList.add(fileAnnotation);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to read file '%s'.", path), e);
        }
        return annotationsList;
    }

    private boolean isSupportedAnnotationType(PDAnnotation annotation) {
        try {
            if (!Arrays.asList(FileAnnotationType.values()).contains(FileAnnotationType.valueOf(annotation.getSubtype()))) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.debug(String.format("Could not parse the FileAnnotation %s into any known FileAnnotationType. It was %s!", annotation, annotation.getSubtype()));
        }
        return true;
    }

    private FileAnnotation createMarkedAnnotations(int pageIndex, PDPage page, PDAnnotation annotation) {
        FileAnnotation annotationBelongingToMarking = new FileAnnotation(
                annotation.getDictionary().getString(COSName.T), FileAnnotation.extractModifiedTime(annotation.getModifiedDate()),
                pageIndex + 1, annotation.getContents(), FileAnnotationType.valueOf(annotation.getSubtype().toUpperCase(Locale.ROOT)), Optional.empty());

        try {
            if (FileAnnotationType.HIGHLIGHT.toString().equals(annotation.getSubtype()) || FileAnnotationType.UNDERLINE.toString().equals(annotation.getSubtype())) {
                annotation.setContents(extractMarkedText(page, annotation));
            }
        } catch (IOException e) {
            annotation.setContents("JabRef: Could not extract any marked text!");
        }

        //Marked text that has a sticky note on it should be linked to the sticky note
        return new FileAnnotation(annotation, pageIndex + 1, annotationBelongingToMarking);
    }


    private String extractMarkedText(PDPage page, PDAnnotation annotation) throws IOException {
        //highlighted or underlined text has to be extracted by the rectangle calculated from the marking
        PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();
        COSArray quadsArray = (COSArray) annotation.getDictionary().getDictionaryObject(COSName.getPDFName("QuadPoints"));
        String markedText = "";
        for (int j = 1,
             k = 0;
             j <= (quadsArray.size() / 8);
             j++) {

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
            stripperByArea.addRegion("markedRegion", rectangle);
            stripperByArea.extractRegions(page);
            String markedTextInLine = stripperByArea.getTextForRegion("markedRegion");

            if (j > 1) {
                markedText = markedText.concat(markedTextInLine);
            } else {
                markedText = markedTextInLine;
            }
        }

        return markedText.trim();
    }

    private boolean validatePath(Path path) {
        Objects.requireNonNull(path);

        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            LOGGER.warn(String.format("File %s does not end with .pdf!", path));
            return false;
        }

        if (!Files.exists(path)) {
            LOGGER.warn(String.format("File %s does not exist!", path));
            return false;
        }

        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            LOGGER.warn(String.format("File %s is not readable!", path));
            return false;
        }

        return true;
    }
}
