package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;

public class RisKeywords implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        KeywordList keywords = KeywordList.parse(s, ',');
        int i = 0;
        for (Keyword keyword : keywords) {
            sb.append("KW  - ");
            sb.append(keyword.toString());
            if (i < (keywords.size() - 1)) {
                sb.append(OS.NEWLINE);
            }
            i++;
        }
        return sb.toString();
    }
}
