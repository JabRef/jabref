package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

public class FormatPagesForHTML implements LayoutFormatter {

    @Override
    public String format(String field) {
        return field.replace("--", "-");
    }
}
