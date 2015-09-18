/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer.fileformat;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.logic.util.strings.StringUtil;

/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
class FieldContentParser {

    /**
     * Performs the reformatting
     * @param content StringBuffer containing the field to format. key contains field name according to field
     *  was edited by Kuehn/Havalevich
     * @return The formatted field content. NOTE: the StringBuffer returned may
     * or may not be the same as the argument given.
     */
    public StringBuffer format(StringBuffer content, String key) {
        int i = 0;

        // Unify line breaks
        content = new StringBuffer(StringUtil.unifyLineBreaks(content.toString()));

        while (i < content.length()) {
            int c = content.charAt(i);
            if (c == '\n') {
                // @formatter:off
                if (content.length() > i + 1 && content.charAt(i + 1) == '\t'
                        && (content.length() == i + 2 || !Character.isWhitespace(content.charAt(i + 2)))) {
                    // We have either \n\t followed by non-whitespace, or \n\t at the
                    // end. Both cases indicate a wrap made by JabRef. Remove and insert space if necessary.
                    // @formatter:on
                    content.deleteCharAt(i); // \n
                    content.deleteCharAt(i); // \t
                    // Add space only if necessary:
                    // Note 2007-05-26, mortenalver: the following line was modified. It previously
                    // didn't add a space if the line break was at i==0. This caused some occurences of
                    // "string1 # { and } # string2" constructs lose the space in front of the "and" because
                    // the line wrap caused a JabRef line break at the start of a value containing the " and ".
                    // The bug was caused by a protective check for i>0 to avoid intexing char -1 in content.
                    if (i == 0 || !Character.isWhitespace(content.charAt(i - 1))) {
                        content.insert(i, ' ');
                        // Increment i because of the inserted character:
                        i++;
                    }
                    // @formatter:off
                } else if (content.length() > i + 3 && content.charAt(i + 1) == '\t'
                        && content.charAt(i + 2) == ' '
                        && !Character.isWhitespace(content.charAt(i + 3))) {
                    // We have \n\t followed by ' ' followed by non-whitespace, which indicates
                    // a wrap made by JabRef <= 1.7.1. Remove:
                    // @formatter:on
                    content.deleteCharAt(i); // \n
                    content.deleteCharAt(i); // \t
                    // Remove space only if necessary:
                    if (i > 0 && Character.isWhitespace(content.charAt(i - 1))) {
                        content.deleteCharAt(i);
                    }
                    // @formatter:off
                } else if (content.length() > i + 3 && content.charAt(i + 1) == '\t'
                        && content.charAt(i + 2) == '\n' && content.charAt(i + 3) == '\t') {
                    // We have \n\t\n\t, which looks like a JabRef-formatted empty line.
                    // Remove the tabs and keep one of the line breaks:
                    // @formatter:on
                    content.deleteCharAt(i + 1); // \t
                    content.deleteCharAt(i + 1); // \n
                    content.deleteCharAt(i + 1); // \t
                    // Skip past the line breaks:
                    i++;

                    // Now, if more \n\t pairs are following, keep each line break. This
                    // preserves several line breaks properly. Repeat until done:
                    while (content.length() > i + 1 && content.charAt(i) == '\n' && content.charAt(i + 1) == '\t') {
                        content.deleteCharAt(i + 1);
                        i++;
                    }
                } else if (content.length() > i + 1 && content.charAt(i + 1) != '\n') {
                    // We have a line break not followed by another line break.
                    // Interpretation before JabRef 2.10:
                    //   line break made by whatever other editor, so we will remove the line break.
                    // Current interpretation:
                    //   keep line break
                    i++;
                } else if (content.length() > i + 1 && content.charAt(i + 1) == '\n') {
                    // we have a line break followed by another line break.
                    // This is a linebreak was manually input by the user
                    // Handling before JabRef 2.10:
                    //   just delete the additional linebreak
                    //   content.deleteCharAt(i+1);
                    // Current interpretation:
                    //   keep line break
                    i++;
                    // do not handle \n again
                    i++;
                } else {
                    i++;
                    //content.deleteCharAt(i);
                }
            } else if (c == ' ') {
                //if ((content.length()>i+2) && (content.charAt(i+1)==' ')) {
                if (i > 0 && content.charAt(i - 1) == ' ') {
                    // We have two spaces in a row. Don't include this one.
                    // Yes, of course we have, but in Filenames it is necessary to have all spaces. :-)
                    // This is the reason why the next lines are required
                    // FIXME: just don't edit some fields rather than hacking every exception?
                    if (key != null && key.equals(GUIGlobals.FILE_FIELD)) {
                        i++;
                    } else {
                        content.deleteCharAt(i);
                    }
                } else {
                    i++;
                }
            } else if (c == '\t') {
                // Remove all tab characters that aren't associated with a line break.
                content.deleteCharAt(i);
            } else {
                i++;
            }
        }

        return content;
    }

    /**
     * Performs the reformatting
     * @param content StringBuffer containing the field to format.
     * @return The formatted field content. NOTE: the StringBuffer returned may
     * or may not be the same as the argument given.
     */
    public StringBuffer format(StringBuffer content) {
        return format(content, null);
    }
}
