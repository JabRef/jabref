package org.jabref.gui.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.entry.BibEntry;

/**
 * Placeholder class for a custom generic type safe dragboard to be used in drag and drop, does not depend on serialization
 * Don't use this class directly. Use the instance provided in {@link StateManager#getLocalDragboard()}
 */
public class CustomLocalDragboard {

    @SuppressWarnings("unchecked") private static final Class<List<BibEntry>> BIB_ENTRIES = (Class<List<BibEntry>>) (Class<?>) List.class;

    private final Map<Class<?>, Object> contents = new HashMap<>();

    /**
     * Puts the value of the concrete class in a map. All previous content stored in the map is removed
     *
     * @param type  The Type of the class
     * @param value The value to store
     */
    public <T> void putValue(Class<T> type, T value) {
        clearAll();
        contents.put(type, type.cast(value));
    }

    public <T> T getValue(Class<T> type) {
        return type.cast(contents.get(type));
    }

    public boolean hasType(Class<?> type) {
        return contents.containsKey(type);
    }

    public void clear(Class<?> type) {
        contents.remove(type);
    }

    public void clearAll() {
        contents.clear();
    }

    /**
     * Puts A List of {@link BibEntry} in the map All previous content is cleared
     *
     * @param entries The list to put
     */
    public void putBibEntries(List<BibEntry> entries) {
        putValue(BIB_ENTRIES, entries);
    }

    /**
     * Get a List of {@link BibEntry} from the dragboard
     *
     * @return List of BibEntry or empty list if no entries are avaiable
     */
    public List<BibEntry> getBibEntries() {
        if (hasBibEntries()) {
            return getValue(BIB_ENTRIES);
        }
        return Collections.emptyList();
    }

    public boolean hasBibEntries() {
        return hasType(BIB_ENTRIES);
    }

    /**
     * Puts A List of {@link PreviewLayout} in the map All previous content is cleared
     *
     * @param previewLayouts The list to put
     */
    public void putPreviewLayouts(List<PreviewLayout> previewLayouts) {
        putValue(DragAndDropDataFormats.PREVIEWLAYOUT_LIST_CLASS, previewLayouts);
    }

    /**
     * Get a List of {@link PreviewLayout} from the dragboard
     *
     * @return List of PreviewLayout or empty list if no entries are avaiable
     */
    public List<PreviewLayout> getPreviewLayouts() {
        if (hasType(DragAndDropDataFormats.PREVIEWLAYOUT_LIST_CLASS)) {
            return getValue(DragAndDropDataFormats.PREVIEWLAYOUT_LIST_CLASS);
        }
        return Collections.emptyList();
    }
}
