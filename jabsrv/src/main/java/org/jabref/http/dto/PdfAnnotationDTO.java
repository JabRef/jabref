package org.jabref.http.dto;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.pdf.FileAnnotation;

/**
 * A data transfer object class for a linked pdf file and its annotations.
 */
public class PdfAnnotationDTO {
    // save a LinkedPDFFileDTO which contains all necessary info to make a request back to the http server (e. g. to update the annotation content)
    // see https://github.com/JabRef/jabmap/issues/21 first before working with the "path" attribute
    private final LinkedPdfFileDTO parentPDFFile; // contains: title, path and parent BibEntry
    private final List<FileAnnotationDTO> fileAnnotations;

    /// @param AnnotatedPDFFile The Path to the PDF file from which this annotation was extracted.
    /// @param parentBibEntry The BibEntry the parent PDF file is linked to.
    /// @param annotations The annotations to send.
    public PdfAnnotationDTO(Path AnnotatedPDFFile, BibEntry parentBibEntry, List<FileAnnotation> annotations) {
        LinkedFile file = new LinkedFile("", AnnotatedPDFFile, "PDF");

        this.parentPDFFile = new LinkedPdfFileDTO(parentBibEntry, file);
        this.fileAnnotations = annotations.stream()
                .map(fileAnnotation -> new FileAnnotationDTO(fileAnnotation.getAuthor(), fileAnnotation.getContent(), fileAnnotation.getAnnotationType()))
                .toList();
    }
}
