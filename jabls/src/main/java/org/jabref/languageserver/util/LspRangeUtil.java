package org.jabref.languageserver.util;

import org.jabref.logic.importer.ParserResult;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

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
}
