package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.AuthorList;

public class RisAuthors implements ParamLayoutFormatter {

    private String arg = "";

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] authors = AuthorList.fixAuthorLastNameFirst(s).split(" and ");
        for (int i = 0; i < authors.length; i++) {
            sb.append(arg);
            sb.append("  - ");
            sb.append(authors[i]);
            if (i < authors.length - 1) {
                sb.append(OS.NEWLINE);
            }
        }
        return sb.toString();
    }

    @Override
    public void setArgument(String arg) {
        this.arg = arg;
    }
}
