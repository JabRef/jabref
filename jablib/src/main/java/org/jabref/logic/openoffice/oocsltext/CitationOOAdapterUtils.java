package org.jabref.logic.openoffice.oocsltext;

import com.sun.star.text.XTextCursor;
import org.jspecify.annotations.NullMarked;

@NullMarked
class CitationOOAdapterUtils {
    private static final String LINE_BREAK = "\\R";

    private CitationOOAdapterUtils() {
    }

    static boolean hasPrecedingSpace(XTextCursor cursor) {
        boolean precedingSpaceExists;
        XTextCursor checkCursor = cursor.getText().createTextCursorByRange(cursor.getStart());

        // Check if we're at the start of the document - if yes we set the flag and don't insert a space
        if (!checkCursor.goLeft((short) 1, true)) {
            // We're at the start of the document
            return true;
        } else {
            // If not at the start of document, check if there is a space before
            precedingSpaceExists = " ".equals(checkCursor.getString());
            // If not a space, check if it's a paragraph break
            if (!precedingSpaceExists) {
                precedingSpaceExists = checkCursor.getString().matches(LINE_BREAK);
            }
        }

        return precedingSpaceExists;
    }

    static boolean hasSucceedingSpace(XTextCursor cursor) {
        boolean succeedingSpaceExists;
        XTextCursor checkCursor = cursor.getText().createTextCursorByRange(cursor.getStart());

        if (!checkCursor.goRight((short) 1, true)) {
            // We're at the end of the line
            return false;
        } else {
            succeedingSpaceExists = " ".equals(checkCursor.getString());
            if (!succeedingSpaceExists) {
                succeedingSpaceExists = checkCursor.getString().matches(LINE_BREAK);
            }
        }

        return succeedingSpaceExists;
    }
}
