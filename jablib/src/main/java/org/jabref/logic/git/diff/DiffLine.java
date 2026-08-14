package org.jabref.logic.git.diff;

import java.util.Optional;

public record DiffLine(
        DiffLineType type,
        Optional<Integer> oldLineNumber,
        Optional<Integer> newLineNumber,
        Optional<String> oldLines,
        Optional<String> newLines
) {
    public static DiffLine context(int oldLineNumber, int newLineNumber, String oldLines, String newLines) {
        return new DiffLine(DiffLineType.CONTEXT, Optional.of(oldLineNumber), Optional.of(newLineNumber), Optional.of(oldLines), Optional.of(newLines));
    }

    public static DiffLine changed(int oldLineNumber, int newLineNumber, String oldLines, String newLines) {
        return new DiffLine(DiffLineType.CHANGED, Optional.of(oldLineNumber), Optional.of(newLineNumber), Optional.of(oldLines), Optional.of(newLines));
    }

    public static DiffLine deleted(int oldLineNumber, String oldLines) {
        return new DiffLine(DiffLineType.DELETED, Optional.of(oldLineNumber), Optional.empty(), Optional.of(oldLines), Optional.empty());
    }

    public static DiffLine added(int newLineNumber, String newLines) {
        return new DiffLine(DiffLineType.ADDED, Optional.empty(), Optional.of(newLineNumber), Optional.empty(), Optional.of(newLines));
    }
}
