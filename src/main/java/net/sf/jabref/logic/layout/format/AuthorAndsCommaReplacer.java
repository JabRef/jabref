package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 * Replaces and's for & (in case of two authors) and , (in case
 * of more than two authors).
 *
 * @author Carlos Silla
 */
public class AuthorAndsCommaReplacer implements LayoutFormatter {

    /* (non-Javadoc)
     * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {

        String[] authors = fieldText.split(" and ");
        String s;

        switch (authors.length) {
        case 1:
            //Does nothing;
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
