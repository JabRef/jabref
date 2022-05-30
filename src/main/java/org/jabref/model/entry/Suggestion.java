package org.jabref.model.entry;

/**
 * This class is used to represent an autocomplete suggestion for the global search bar. It stores both the value and type of suggestion.
 */
public class Suggestion {

    private final String value;
    private final Class type;

    public Suggestion(String v, Class t) {
        this.value = v;
        this.type = t;
    }

    public String getValue() {
        return value;
    }

    public Class getType() {
        return type;
    }
}
