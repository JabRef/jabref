package net.sf.jabref.event;


public class ChangedFieldEvent {

    private final String fieldName;
    private final String value;

    public ChangedFieldEvent(String fieldName, String newValue) {
        this.fieldName = fieldName;
        this.value = newValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getValue() {
        return value;
    }

}
