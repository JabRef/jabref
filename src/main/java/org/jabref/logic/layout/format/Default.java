package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

/**
 * Layout formatter that puts in a default value (given as argument) if the field is empty.
 * Empty means null or an empty string.
 */
public class Default implements ParamLayoutFormatter {

    private String defValue = "";

    @Override
    public void setArgument(String arg) {
        this.defValue = arg;
    }

    @Override
    public String format(String fieldText) {
        return ((fieldText == null) || fieldText.isEmpty()) ? defValue : fieldText;
    }
}
