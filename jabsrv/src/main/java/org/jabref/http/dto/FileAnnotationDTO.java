package org.jabref.http.dto;

import org.jabref.model.pdf.FileAnnotationType;

public record FileAnnotationDTO(
        String author,
        String content,
        FileAnnotationType type
) {
    public FileAnnotationDTO {
        if (author == null || author.isBlank()) {
            author = "(N/A)";
        }
        if (content == null || content.isBlank()) {
            content = "(N/A)";
        }
        if (type == null) {
            type = FileAnnotationType.UNKNOWN;
        }
    }
}
