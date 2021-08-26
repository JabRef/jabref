package org.jabref.logic.openoffice.backend;

import com.sun.star.text.XTextCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for processing OO Writer documents.
 */
public class GetContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetContext.class);

    private GetContext() {
        // Just to hide the public constructor
    }

    /**
     *  Get the text belonging to cursor with up to
     *  charBefore and charAfter characters of context.
     *
     *  The actual context may be smaller than requested.
     *
     *  @param cursor
     *  @param charBefore Number of characters requested.
     *  @param charAfter  Number of characters requested.
     *  @param htmlMarkup If true, the text belonging to the reference mark is surrounded by bold
     *                    html tag.
     *
     */
    public static String getCursorStringWithContext(XTextCursor cursor,
                                                    int charBefore,
                                                    int charAfter,
                                                    boolean htmlMarkup) {

        String citPart = cursor.getString();

        // extend cursor range left
        int flex = 8;
        for (int i = 0; i < charBefore; i++) {
            try {
                cursor.goLeft((short) 1, true);
                // If we are close to charBefore and see a space, then cut here. Might avoid cutting
                // a word in half.
                if ((i >= (charBefore - flex))
                    && Character.isWhitespace(cursor.getString().charAt(0))) {
                    break;
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.warn("Problem going left", ex);
            }
        }

        int lengthWithBefore = cursor.getString().length();
        int addedBefore = lengthWithBefore - citPart.length();

        cursor.collapseToStart();
        for (int i = 0; i < (charAfter + lengthWithBefore); i++) {
            try {
                cursor.goRight((short) 1, true);
                if (i >= ((charAfter + lengthWithBefore) - flex)) {
                    String strNow = cursor.getString();
                    if (Character.isWhitespace(strNow.charAt(strNow.length() - 1))) {
                        break;
                    }
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.warn("Problem going right", ex);
            }
        }

        String result = cursor.getString();
        if (htmlMarkup) {
            result = (result.substring(0, addedBefore)
                      + "<b>" + citPart + "</b>"
                      + result.substring(lengthWithBefore));
        }
        return result.trim();
    }

}
