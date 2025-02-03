package org.jabref.logic.quality.consistency;

public record ConsistencyMessage(String message) implements Cloneable {

    @Override
    public String toString() {
        return "[" + message() + "]";
    }

    @Override
    public Object clone() {
        return new ConsistencyMessage(message);
    }
}
