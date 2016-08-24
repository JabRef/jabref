package net.sf.jabref.logic.layout.format;

import java.util.Set;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.util.OS;

public class RisKeywords implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Set<String> keywords = net.sf.jabref.model.entry.EntryUtil.getSeparatedKeywords(s);
        int i = 0;
        for (String keyword : keywords) {
            sb.append("KW  - ");
            sb.append(keyword);
            if (i < (keywords.size() - 1)) {
                sb.append(OS.NEWLINE);
            }
            i++;
        }
        return sb.toString();
    }
}
