package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

public class FormatPagesForXML implements LayoutFormatter {

    @Override
    public String format(String field) {
        return field.replace("--", "&#x2013;");
    }
}
