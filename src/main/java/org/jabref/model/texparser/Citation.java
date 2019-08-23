package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.Objects;

public class Citation {

    /**
     * The total number of characters that are shown around a cite (cite width included).
     */
    private static final int CONTEXT_WIDTH = 300;

    private final Path path;
    private final int line;
    private final int colStart;
    private final int colEnd;
    private final String lineText;

    public Citation(Path path, int line, int colStart, int colEnd, String lineText) {
        if (line <= 0) {
            throw new IllegalArgumentException("Line has to be greater than 0.");
        }

        if (colStart < 0 || colEnd > lineText.length()) {
            throw new IllegalArgumentException("Citation has to be between 0 and line length.");
        }

        this.path = Objects.requireNonNull(path);
        this.line = line;
        this.colStart = colStart;
        this.colEnd = colEnd;
        this.lineText = lineText;
    }

    public Path getPath() {
        return path;
    }

    public int getLine() {
        return line;
    }

    public int getColStart() {
        return colStart;
    }

    public int getColEnd() {
        return colEnd;
    }

    public String getLineText() {
        return lineText;
    }

    /**
     * Get a fixed-width string that contains a cite and the text that surrounds it along the same line.
     */
    public String getContext() {
        int center = (colStart + colEnd) / 2;
        int lineLength = lineText.length();

        int start = Math.max(0, (center + CONTEXT_WIDTH / 2 < lineLength)
                ? center - CONTEXT_WIDTH / 2
                : lineLength - CONTEXT_WIDTH);
        int end = Math.min(lineLength, start + CONTEXT_WIDTH);

        // Add three dots when the string does not contain all the line.
        return String.format("%s%s%s",
                (start > 0) ? "..." : "",
                lineText.substring(start, end).trim(),
                (end < lineLength) ? "..." : "");
    }

    @Override
    public String toString() {
        return String.format("Citation{path=%s, line=%s, colStart=%s, colEnd=%s, lineText='%s'}",
                this.path,
                this.line,
                this.colStart,
                this.colEnd,
                this.lineText);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Citation that = (Citation) obj;

        return Objects.equals(path, that.path)
                && Objects.equals(line, that.line)
                && Objects.equals(colStart, that.colStart)
                && Objects.equals(colEnd, that.colEnd)
                && Objects.equals(lineText, that.lineText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, line, colStart, colEnd, lineText);
    }
}
