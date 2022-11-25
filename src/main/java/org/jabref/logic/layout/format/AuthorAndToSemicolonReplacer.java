package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

public class AuthorAndToSemicolonReplacer implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return fieldText.replaceAll(" and ", "; ");
    }
}
