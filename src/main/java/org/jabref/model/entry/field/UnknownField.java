package org.jabref.model.entry.field;

public class UnknownField implements Field {
    private final String name;

    public UnknownField(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
