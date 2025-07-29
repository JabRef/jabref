package org.jabref.http.server.languageserver;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class LSPHelper {

    /// Transform startindex and endindex (jabref) to column and line (lsp)
    public static Range getRangeFromIndices(String text, int startIdx, int endIdx) {
        String[] lines = text.split("\\n", -1);
        int currIdx = 0;

        Position start = null;
        Position end = null;

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            int lineLength = lines[lineNum].length() + 1;

            if (start == null && currIdx + lineLength > startIdx) {
                start = new Position(lineNum, startIdx - currIdx);
            }

            if (currIdx + lineLength > endIdx) {
                end = new Position(lineNum, endIdx - currIdx);
                break;
            }

            currIdx += lineLength;
        }

        if (start == null || end == null) {
            throw new IllegalArgumentException("Indices out of the allowed bounds");
        }

        return new Range(start, end);
    }
}
