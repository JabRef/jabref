package org.jabref.logic.importer.fileformat.mods;

public class IdentifierDefn {
    private String type;
    private String value;

    public IdentifierDefn() {
        type = "";
        value = "";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null) {
            this.type = type;
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
