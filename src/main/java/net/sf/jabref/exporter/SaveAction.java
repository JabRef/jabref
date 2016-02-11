package net.sf.jabref.exporter;

import net.sf.jabref.logic.formatter.Formatter;

/**
 * Defines a mapping between a formatter and a field for which a save action can be applied
 */
public final class SaveAction {

    private final String fieldName;

    private final Formatter formatter;

    public SaveAction(String fieldName, Formatter formatter) {
        this.fieldName = fieldName;
        this.formatter = formatter;

    }

    public Formatter getFormatter() {
        return formatter;
    }

    public String getFieldName() {
        return fieldName;
    }

}
