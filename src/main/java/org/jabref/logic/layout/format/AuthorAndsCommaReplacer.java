package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/**
 * Replaces and's for & (in case of two authors) and , (in case of more than two authors).
 */
public class AuthorAndsCommaReplacer implements LayoutFormatter {

    @Override
    public String format(String fieldText) {

        String[] authors = fieldText.split(" and ");
        String s;

        switch (authors.length) {
            case 1:
                // Does nothing
                s = authors[0];
                break;
            case 2:
                s = authors[0] + " & " + authors[1];
                break;
            default:
                int i;
                int x = authors.length;
                StringBuilder sb = new StringBuilder();

                for (i = 0; i < (x - 2); i++) {
                    sb.append(authors[i]).append(", ");
                }
                sb.append(authors[i]).append(" & ").append(authors[i + 1]);
                s = sb.toString();
                break;
        }

        return s;
    }
}
