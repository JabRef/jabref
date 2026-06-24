package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.pdf.FileAnnotation;

import static org.jabref.logic.util.strings.StringUtil.quoteForHTML;

public class FileAnnotationPreview {
    public static String render(@Nullable Map<Path, List<FileAnnotation>> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<br><br><b>")
            .append(Localization.lang("PDF Annotations"))
            .append("</b><br>");

        annotations.entrySet().stream()
                   .filter(entry -> entry.getKey() != null && entry.getKey().getFileName() != null)
                   .forEach(entry -> {
                       List<FileAnnotation> fileAnnotations = entry.getValue();
                       if (fileAnnotations == null || fileAnnotations.isEmpty()) {
                           return;
                       }

                       String fileName = entry.getKey().getFileName().toString();
                       html.append("<br><i>").append(quoteForHTML(fileName)).append("</i><br>");

                       fileAnnotations.stream()
                                      .filter(Objects::nonNull)
                                      .filter(annotation -> StringUtil.isNotBlank(annotation.getContent()))
                                      .sorted(Comparator.comparingInt(FileAnnotation::getPage))
                                      .forEach(annotation -> renderAnnotation(html, annotation));
                   });

        return html.toString();
    }

    private static void renderAnnotation(StringBuilder html, FileAnnotation annotation) {
        String typeStr;

        if (annotation.getAnnotationType() != null) {
            String typeName = annotation.getAnnotationType().toString();

            if ("HIGHLIGHT".equalsIgnoreCase(typeName)) {
                typeStr = Localization.lang("highlight");
            } else if ("UNDERLINE".equalsIgnoreCase(typeName)) {
                typeStr = Localization.lang("underline");
            } else if ("STRIKEOUT".equalsIgnoreCase(typeName)) {
                typeStr = Localization.lang("strikeout");
            } else {
                typeStr = typeName.toLowerCase();
            }
        } else {
            typeStr = Localization.lang("unknown");
        }

        int page = annotation.getPage();
        String content = annotation.getContent();
        String headerLabel = Localization.lang("%0 (page %1)", typeStr, String.valueOf(page));

        html.append("<b>")
            .append(quoteForHTML(headerLabel))
            .append("</b> ")
            .append(quoteForHTML(content));

        if (annotation.hasLinkedAnnotation() && annotation.getLinkedFileAnnotation() != null) {
            String noteContent = annotation.getLinkedFileAnnotation().getContent();

            if (StringUtil.isNotBlank(noteContent)) {
                String noteLabel = Localization.lang("Note");
                String formattedNote = " — %s: %s".formatted(noteLabel, noteContent);

                html.append("<i>")
                    .append(quoteForHTML(formattedNote))
                    .append("</i>");
            }
        }

        html.append("<br>");
    }
}
