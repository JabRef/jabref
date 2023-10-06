package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;

public class HayagrivayamlAuthors implements LayoutFormatter {
    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] authors = AuthorList.fixAuthorLastNameFirst(s).split(" and ");
        sb.append("[");
        for (int i = 0; i < authors.length; i++) {
            sb.append("\"");
            sb.append(authors[i]);
            sb.append("\"");
            if (i < authors.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");

        return sb.toString();
    }
}
