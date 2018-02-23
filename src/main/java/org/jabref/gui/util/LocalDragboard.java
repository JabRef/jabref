package org.jabref.gui.util;

import java.util.HashMap;
import java.util.Map;

public enum LocalDragboard {

    INSTANCE;

    private final Map<Class<?>, Object> contents;


    private LocalDragboard() {
        this.contents = new HashMap<>();
    }

    public <T> void putValue(Class<T> type, T value) {
        contents.put(type, type.cast(value));
    }

    public <T> T getValue(Class<T> type) {
        return type.cast(contents.get(type));
    }

    public boolean hasType(Class<?> type) {
        return contents.keySet().contains(type);
    }

    public void clear(Class<?> type) {
        contents.remove(type);
    }

    public void clearAll() {
        contents.clear();
    }
}