package net.sf.jabref.imports;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
public class FieldContentParser {

    private static Pattern wrap = Pattern.compile("\\n\\t");

    /**
     * Performs the reformatting
     * @param content StringBuffer containing the field to format.
     * @return The formatted field content. NOTE: the StringBuffer returned may
     * or may not be the same as the argument given.
     */
    public StringBuffer format(StringBuffer content) {

        int prev = -1;
        int i=0;
        while (i<content.length()) {

            int c = content.charAt(i);
            if (c == '\n') {

                if ((content.length()>i+2) && (content.charAt(i+1)=='\t')
                    && !Character.isWhitespace(content.charAt(i+2))) {
                    // We have \n\t followed by non-whitespace, which indicates
                    // a wrap made by JabRef. Remove and insert space if necessary.

                    content.deleteCharAt(i); // \n
                    content.deleteCharAt(i); // \t
                    // Add space only if necessary:
                    if ((i>0) && !Character.isWhitespace(content.charAt(i-1))) {
                        content.insert(i-1, ' ');
                        // Increment i because of the inserted character:
                        i++;
                    }
                }
                if ((content.length()>i+3) && (content.charAt(i+1)=='\t')
                    && (content.charAt(i+2)==' ')
                    && !Character.isWhitespace(content.charAt(i+3))) {
                    // We have \n\t followed by ' ' followed by non-whitespace, which indicates
                    // a wrap made by JabRef <= 1.7.1. Remove:

                    content.deleteCharAt(i); // \n
                    content.deleteCharAt(i); // \t
                    // Remove space only if necessary:
                    if ((i>0) && Character.isWhitespace(content.charAt(i-1))) {
                        content.deleteCharAt(i);
                    }
                }
                else if ((content.length()>i+3) && (content.charAt(i+1)=='\t')
                        && (content.charAt(i+2)=='\n') && (content.charAt(i+3)=='\t')) {
                    // We have \n\t\n\t, which looks like a JabRef-formatted empty line.
                    // Remove the tabs and keep the line breaks:
                    content.deleteCharAt(i+1);
                    content.deleteCharAt(i+2); // i+3 is now i+2 since we removed one char.
                    // Skip past the line breaks:
                    i += 2;
                }
                else
                    i++;
                //content.deleteCharAt(i);
            }
            else
                i++;
            prev = c;

        }

        return content;
    }
}
