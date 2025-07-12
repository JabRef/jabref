package org.jabref.http.dto;

import org.jabref.model.pdf.FileAnnotationType;
import org.jabref.model.strings.StringUtil;

public record FileAnnotationDTO(
        String author,
        String content,
        FileAnnotationType type
) {
    public FileAnnotationDTO {
        if (StringUtil.isBlank(author)) {
            author = "(N/A)";
        }
        if (StringUtil.isBlank(content)) {
            content = "(N/A)";
        }
        if (type == null) {
            type = FileAnnotationType.UNKNOWN;
        }
    }
}
