package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/**
 * Remove non printable character formatter.
 *
 * Based on the RemoveBrackets.java class (Revision 1.2) by mortenalver
 */
public class RemoveWhitespace implements LayoutFormatter {

    @Override
    public String format(String fieldEntry) {

        if (fieldEntry == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(fieldEntry.length());

        for (char c : fieldEntry.toCharArray()) {
            if (!Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
