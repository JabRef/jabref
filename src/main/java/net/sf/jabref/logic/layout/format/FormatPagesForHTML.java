package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

public class FormatPagesForHTML implements LayoutFormatter {

    @Override
    public String format(String field) {
        return field.replace("--", "-");
    }
}
