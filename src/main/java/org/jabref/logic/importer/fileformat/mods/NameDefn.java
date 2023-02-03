package org.jabref.logic.importer.fileformat.mods;

public class NameDefn {
    private String value;
    private String type;

    public NameDefn(String value, String type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
