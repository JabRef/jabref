package org.jabref.model.texparser;

import java.nio.file.Path;

import org.jspecify.annotations.NonNull;

/**
 *
 * @param path the path to the file containing the citation
 * @param line the line number of the citation (starting at 1)
 * @param colStart the column number of the start of the citation (starting at 1)
 * @param colEnd the column number of the end of the citation (starting at 1)
 * @param lineText the text of the line containing the citation
 */
public record Citation(Path path, int line, int colStart, int colEnd, String lineText) {
    /**
     * The total number of characters that are shown around a cite (cite width included).
     */
    private static final int CONTEXT_WIDTH = 300;

    public Citation(@NonNull Path path, int line, int colStart, int colEnd, String lineText) {
        if (line <= 0) {
            throw new IllegalArgumentException("Line has to be greater than 0.");
        }

        if (colStart < 0 || colEnd > lineText.length()) {
            throw new IllegalArgumentException("Citation has to be between 0 and line length.");
        }

        this.path = path;
        this.line = line;
        this.colStart = colStart;
        this.colEnd = colEnd;
        this.lineText = lineText;
    }

    /**
     * Get a fixed-width string that contains a cite and the text that surrounds it along the same line.
     */
    public String getContext() {
        int center = (colStart + colEnd) / 2;
        int lineLength = lineText.length();

        int start = Math.max(0, center + CONTEXT_WIDTH / 2 < lineLength
                                ? center - CONTEXT_WIDTH / 2
                                : lineLength - CONTEXT_WIDTH);
        int end = Math.min(lineLength, start + CONTEXT_WIDTH);

        // Add three dots when the string does not contain all the line.
        return "%s%s%s".formatted(
                start > 0 ? "..." : "",
                lineText.substring(start, end).trim(),
                end < lineLength ? "..." : "");
    }

    @Override
    public String toString() {
        return "Citation{path=%s, line=%s, colStart=%s, colEnd=%s, lineText='%s'}".formatted(
                this.path,
                this.line,
                this.colStart,
                this.colEnd,
                this.lineText);
    }
}
