package org.jabref.languageserver.util;

import org.jabref.logic.importer.ParserResult;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/// Because only postions are supported by the lsp https://github.com/microsoft/language-server-protocol/issues/96 we need to convert back and forth
public class LspRangeUtil {

    public static int toOffset(String content, Position pos) {
        int line = Math.max(0, pos.getLine());
        int character = Math.max(0, pos.getCharacter());

        int currentLine = 0;
        int i = 0;
        int lineStart = 0;

        while (i < content.length() && currentLine < line) {
            char c = content.charAt(i++);
            if (c == '\n') {
                lineStart = i;
                currentLine++;
            }
        }

        if (currentLine < line) {
            return content.length();
        }

        int lineEnd = content.indexOf('\n', lineStart);
        if (lineEnd == -1) {
            lineEnd = content.length();
        }

        int offset = lineStart + Math.min(character, Math.max(0, lineEnd - lineStart));
        return Math.max(0, Math.min(content.length(), offset));
    }

    public static Range convertToLspRange(ParserResult.Range range) {
        return new Range(
                new Position(Math.max(range.startLine() - 1, 0), Math.max(range.startColumn() - 1, 0)),
                new Position(Math.max(range.endLine() - 1, 0), Math.max(range.endColumn() - 1, 0))
        );
    }

    /**
     *
     * @param line line starting at 1
     * @param colStart column starting at 1
     * @param colEnd column starting at 1
     * @return the LSP4J Range of the line and columns
     */
    public static Range convertToLspRange(int line, int colStart, int colEnd) {
        return new Range(new Position(line - 1, colStart), new Position(line - 1, colEnd));
    }

    public static Range convertToLspRange(String content, int startIndex, int endIndex) {
        Position start = convertToLspPosition(content, startIndex);
        Position end = convertToLspPosition(content, endIndex);
        return new Range(start, end);
    }

    public static Position convertToLspPosition(String content, int index) {
        int clampedIndex = Math.max(0, Math.min(content.length(), index));
        int line = 0;
        int column = 0;

        for (int i = 0; i < clampedIndex; i++) {
            if (content.charAt(i) == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }

        return new Position(line, column);
    }

    public static boolean isPositionInRange(Position position, Range range) {
        return isRangeInRange(range, new Range(position, position));
    }

    public static boolean isRangeInRange(Range outer, Range inner) {
        return comparePositions(outer.getStart(), inner.getStart()) <= 0
                && comparePositions(outer.getEnd(), inner.getEnd()) >= 0;
    }

    public static int comparePositions(Position p1, Position p2) {
        if (p1.getLine() != p2.getLine()) {
            return Integer.compare(p1.getLine(), p2.getLine());
        }
        return Integer.compare(p1.getCharacter(), p2.getCharacter());
    }
}
