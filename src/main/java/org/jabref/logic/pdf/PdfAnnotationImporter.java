package org.jabref.logic.pdf;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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


public class PdfAnnotationImporter implements AnnotationImporter {

    private static final Log LOGGER = LogFactory.getLog(PdfAnnotationImporter.class);

    /**
     * Imports the comments from a pdf specified by its path
     *
     * @param path a path to a pdf
     * @return a list with the all the annotations found in the file of the path
     */
    @Override
    public List<FileAnnotation> importAnnotations(final Path path, final BibDatabaseContext context) {

        Optional<Path> validatePath = validatePath(path, context);
        if (!validatePath.isPresent()) {
            // Path could not be validated, return default result
            return Collections.emptyList();
        }

        Path validPath = validatePath.get();

        List<FileAnnotation> annotationsList = new LinkedList<>();
        try (PDDocument document = PDDocument.load(validPath.toString())) {
            List pdfPages = document.getDocumentCatalog().getAllPages();
            for (int pageIndex = 0; pageIndex < pdfPages.size(); pageIndex++) {
                PDPage page = (PDPage) pdfPages.get(pageIndex);
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation.getSubtype().equals(FDFAnnotationHighlight.SUBTYPE)) {
                        annotationsList.addAll(createHighlightAnnotations(pageIndex, page, annotation));
                    } else {
                        annotationsList.add(new FileAnnotation(annotation, pageIndex + 1));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to read file '%s'.", validPath), e);
        }
        return annotationsList;
    }

    private Collection<FileAnnotation> createHighlightAnnotations(int pageIndex, PDPage page, PDAnnotation annotation) {
        Collection<FileAnnotation> highlightAnnotations = new LinkedList<>();
        FileAnnotation annotationBelongingToHighlighting = new FileAnnotation(
                annotation.getDictionary().getString(COSName.T), FileAnnotation.extractModifiedTime(annotation.getModifiedDate()),
                pageIndex + 1, annotation.getContents(), FDFAnnotationText.SUBTYPE, Optional.empty());
        highlightAnnotations.add(annotationBelongingToHighlighting);

        try {
            annotation.setContents(extractHighlightedText(page, annotation));
        } catch (IOException e) {
            annotation.setContents("JabRef: Could not extract any highlighted text!");
        }

        //highlighted text that has a sticky note on it should be linked to the sticky note
        highlightAnnotations.add(new FileAnnotation(annotation, pageIndex + 1, annotationBelongingToHighlighting));
        return highlightAnnotations;
    }

    private String extractHighlightedText(PDPage page, PDAnnotation annotation) throws IOException {
        //highlighted text has to be extracted by the rectangle calculated from the highlighting
        PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();
        COSArray quadsArray = (COSArray) annotation.getDictionary().getDictionaryObject(COSName.getPDFName("QuadPoints"));
        String highlightedText = "";
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
            stripperByArea.addRegion("highlightedRegion", rectangle);
            stripperByArea.extractRegions(page);
            String highlightedTextInLine = stripperByArea.getTextForRegion("highlightedRegion");

            if (j > 1) {
                highlightedText = highlightedText.concat(highlightedTextInLine);
            } else {
                highlightedText = highlightedTextInLine;
            }
        }

        if (highlightedText.trim().isEmpty()) {
            highlightedText = "JabRef: The highlighted area does not contain any legible text!";
        }

        return highlightedText;
    }

    private Optional<Path> validatePath(Path path, BibDatabaseContext context) {
        Objects.requireNonNull(path);

        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            LOGGER.warn(String.format("File %s does not end with .pdf!", path.toString()));
            return Optional.empty();
        }

        if (!Files.exists(path)) {
            Optional<File> importedFile = FileUtil.expandFilename(context, path.toString(),
                    JabRefPreferences.getInstance().getFileDirectoryPreferences());
            if (importedFile.isPresent()) {
                // No need to check file existence again, because it is done in FileUtil.expandFilename()
                return Optional.of(Paths.get(importedFile.get().getAbsolutePath()));
            } else {
                //return empty list to recognize invalid import
                return Optional.empty();
            }
        }

        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            LOGGER.warn(String.format("File %s is not readable!", path.toString()));
            return Optional.empty();
        }
        return Optional.of(path);
    }
}
