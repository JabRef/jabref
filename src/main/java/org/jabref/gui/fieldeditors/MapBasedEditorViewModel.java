package org.jabref.gui.fieldeditors;

import java.util.ArrayList;
import java.util.List;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;

import com.google.common.collect.BiMap;

/**
 * View model for a field editor that shows various options backed by a map.
 */
public abstract class MapBasedEditorViewModel<T> extends OptionEditorViewModel<T> {

    public MapBasedEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider) {
        super(fieldName, suggestionProvider);
    }

    protected abstract BiMap<String, T> getItemMap();

    @Override
    public StringConverter<T> getStringConverter() {
        return new StringConverter<T>() {
            @Override
            public String toString(T object) {
                if (object == null) {
                    return null;
                } else {
                    return getItemMap().inverse().getOrDefault(object, "");
                }
            }

            @Override
            public T fromString(String string) {
                if (string == null) {
                    return null;
                } else {
                    return getItemMap().getOrDefault(string, null);
                }
            }
        };
    }

    @Override
    public List<T> getItems() {
        return new ArrayList<>(getItemMap().values());
    }
}
