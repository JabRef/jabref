package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.pdf.FileAnnotation;

public class FileAnnotationHtmlRenderer {

    private FileAnnotationHtmlRenderer() {
        // utility class
    }

    // Renders PDF annotations as an HTML string for the entry preview
    // @param annotations map of file paths to their annotations
    // @return formatted HTML string, or empty string if no annotations exist

    public static String render(Map<Path, List<FileAnnotation>> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<BR><BR><b>PDF Annotations</b><BR>");

        for (Map.Entry<Path, List<FileAnnotation>> fileEntry : annotations.entrySet()) {
            List<FileAnnotation> fileAnnotations = fileEntry.getValue();
            if (fileAnnotations == null || fileAnnotations.isEmpty()) {
                continue;
            }

            String fileName = fileEntry.getKey().getFileName().toString();
            html.append("<BR><i>").append(escapeHtml(fileName)).append("</i><BR>");

            fileAnnotations.stream().sorted(Comparator.comparingInt(FileAnnotation::getPage)).filter(annotation -> StringUtil.isNotBlank(annotation.getContent())).forEach(annotation -> renderAnnotation(html, annotation));
        }

        return html.toString();
    }

    private static void renderAnnotation(StringBuilder html, FileAnnotation annotation) {
        String type = annotation.getAnnotationType().toString();
        int page = annotation.getPage();
        String content = annotation.getContent();

        html.append("<b>").append(escapeHtml(type)).append(" (p. ").append(page).append("):</b> ");

        if (annotation.hasLinkedAnnotation()) {
            // highlights/underlines with a sticky note attached
            html.append(escapeHtml(content));
            String noteContent = annotation.getLinkedFileAnnotation().getContent();
            if (StringUtil.isNotBlank(noteContent)) {
                html.append(" — <i>Note: ").append(escapeHtml(noteContent)).append("</i>");
            }
        } else {
            html.append(escapeHtml(content));
        }

        html.append("<BR>");
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
