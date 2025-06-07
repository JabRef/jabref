package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Remove non-printable character formatter.
 */
@NullMarked
public class NonSpaceWhitespaceRemover implements LayoutFormatter {

    @Override
    public @Nullable String format(@Nullable String fieldEntry) {
        if (fieldEntry == null) {
            return null;
        }

        int length = fieldEntry.length();
        int firstRemoveIndex = -1;

        // First pass: detect the first whitespace to avoid building anything unnecessarily
        for (int i = 0; i < length; i++) {
            char c = fieldEntry.charAt(i);
            if (!shouldKeep(c)) {
                firstRemoveIndex = i;
                break;
            }
        }

        // No whitespace? Return original string to avoid allocations
        if (firstRemoveIndex == -1) {
            return fieldEntry;
        }

        // Build result string, starting from the first detected whitespace
        StringBuilder sb = new StringBuilder(length - 1);
        for (int i = 0; i < length; i++) {
            char c = fieldEntry.charAt(i);
            if (shouldKeep(c)) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static boolean shouldKeep(char c) {
        return !Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
}
