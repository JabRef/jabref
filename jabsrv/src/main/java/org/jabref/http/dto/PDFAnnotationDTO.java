package org.jabref.http.dto;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.pdf.FileAnnotation;

/**
 * A data transfer object class for a linked pdf file and it's annotations.
 */
public class PDFAnnotationDTO {
    // save a LinkedPDFFileDTO which contains all necessary info to make a request back to the http server (e. g. to update the annotation content)
    // see https://github.com/JabRef/jabmap/issues/21 first before working with the "path" attribute
    private final LinkedPdfFileDTO parentPDFFile; // contains: title, path and parent BibEntry
    private final List<FileAnnotationDTO> fileAnnotations;

    /// @param annotation The annotation to send.
    /// @param pathToParentPDFFile The Path to the PDF file from which this annotation was extracted.
    /// @param parentBibEntry The BibEntry the parent PDF file is linked to.
    public PDFAnnotationDTO(Path pathToParentPDFFile, BibEntry parentBibEntry, List<FileAnnotation> annotation) {
        LinkedFile file = new LinkedFile("", pathToParentPDFFile, "PDF");

        this.parentPDFFile = new LinkedPdfFileDTO(parentBibEntry, file);
        this.fileAnnotations = annotation.stream()
                .map(fileAnnotation -> new FileAnnotationDTO(fileAnnotation.getAuthor(), fileAnnotation.getContent(), fileAnnotation.getAnnotationType()))
                .toList();
    }
}
