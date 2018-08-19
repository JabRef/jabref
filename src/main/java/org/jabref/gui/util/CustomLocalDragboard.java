package org.jabref.gui.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.GUIGlobals;
import org.jabref.model.entry.BibEntry;

/**
 * Placebolder class for a custom dragboard to be used in drag and drop, does not depend on serialization
 * Don't use this class directly. Use the instance provided in {@link GUIGlobals#localDragboard}
 */
public class CustomLocalDragboard {

    private final Map<Class<?>, Object> contents = new HashMap<>();

    public <T> void putValue(Class<T> type, T value) {
        clearAll();
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

    public void putBibEntries(List<BibEntry> entries) {
        putValue(DragAndDropDataFormats.BIBENTRY_LIST_CLASS, entries);
    }

    /**
     * Get a List of {@link BibEntry} from the dragboard
     * @return List of BibEntry or empty list if no entries are avaiable
     */
    public List<BibEntry> getBibEntries() {
        if (hasType(DragAndDropDataFormats.BIBENTRY_LIST_CLASS)) {
            return getValue(DragAndDropDataFormats.BIBENTRY_LIST_CLASS);
        }
        return Collections.emptyList();
    }

}