package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.Objects;

public class Citation {

    /**
     * The total number of characters that are shown around a cite (cite width included).
     */
    private static final int CONTEXT_WIDTH = 50;

    private final Path path;
    private final int line;
    private final int colStart;
    private final int colEnd;
    private final String lineText;

    public Citation(Path path, int line, int colStart, int colEnd, String lineText) {
        this.path = path;
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
     * Get a fixed-width string that shows the context of a citation.
     *
     * @return String that contains a cite and the text that surrounds it along the same line.
     */
    public String getContext() {
        int center = (colStart + colEnd) / 2;
        int lineLength = lineText.length();

        int start = Math.max(0, (center + CONTEXT_WIDTH / 2 < lineLength)
                ? center - CONTEXT_WIDTH / 2
                : lineLength - CONTEXT_WIDTH);
        int end = Math.min(lineLength, start + CONTEXT_WIDTH);

        return lineText.substring(start, end);
    }

    @Override
    public String toString() {
        return String.format("%s (%d:%d-%d) \"%s\"", path, line, colStart, colEnd, getContext());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Citation citation = (Citation) o;

        return path.equals(citation.path)
                && line == citation.line
                && colStart == citation.colStart
                && colEnd == citation.colEnd
                && lineText.equals(citation.lineText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, line, colStart, colEnd, lineText);
    }
}
