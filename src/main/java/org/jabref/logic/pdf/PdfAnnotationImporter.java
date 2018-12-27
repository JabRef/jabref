package org.jabref.logic.pdf;

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

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfAnnotationImporter implements AnnotationImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfAnnotationImporter.class);

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
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDPageTree pdfPages = document.getDocumentCatalog().getPages();
            for (int pageIndex = 0; pageIndex < pdfPages.getCount(); pageIndex++) {
                PDPage page = pdfPages.get(pageIndex);
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (!isSupportedAnnotationType(annotation)) {
                        continue;
                    }

                    if (FileAnnotationType.isMarkedFileAnnotationType(annotation.getSubtype())) {
                        annotationsList.add(createMarkedAnnotations(pageIndex, page, annotation));
                    } else {
                        FileAnnotation fileAnnotation = new FileAnnotation(annotation, pageIndex + 1);
                        if ((fileAnnotation.getContent() != null) && !fileAnnotation.getContent().isEmpty()) {
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
        if (annotation.getSubtype() == null) {
            return false;
        }
        if ("Link".equals(annotation.getSubtype()) || "Widget".equals(annotation.getSubtype())) {
            LOGGER.debug(annotation.getSubtype() + " is excluded from the supported file annotations");
            return false;
        }
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
                annotation.getCOSObject().getString(COSName.T), FileAnnotation.extractModifiedTime(annotation.getModifiedDate()),
                pageIndex + 1, annotation.getContents(), FileAnnotationType.valueOf(annotation.getSubtype().toUpperCase(Locale.ROOT)), Optional.empty());

        if (annotationBelongingToMarking.getAnnotationType().isLinkedFileAnnotationType()) {
            try {
                COSArray boundingBoxes = (COSArray) annotation.getCOSObject().getDictionaryObject(COSName.getPDFName("QuadPoints"));
                annotation.setContents(new TextExtractor(page, boundingBoxes).extractMarkedText());
            } catch (IOException e) {
                annotation.setContents("JabRef: Could not extract any marked text!");
            }
        }

        //Marked text that has a sticky note on it should be linked to the sticky note
        return new FileAnnotation(annotation, pageIndex + 1, annotationBelongingToMarking);
    }

    private boolean validatePath(Path path) {
        Objects.requireNonNull(path);

        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            LOGGER.warn(String.format("File '%s' does not end with .pdf!", path));
            return false;
        }

        if (!Files.exists(path)) {
            LOGGER.warn(String.format("File '%s' does not exist!", path));
            return false;
        }

        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            LOGGER.warn(String.format("File '%s' is not readable!", path));
            return false;
        }

        return true;
    }
}
