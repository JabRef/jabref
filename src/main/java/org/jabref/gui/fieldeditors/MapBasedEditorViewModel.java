package org.jabref.gui.fieldeditors;

import java.util.ArrayList;
import java.util.List;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.BiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View model for a field editor that shows various options backed by a map.
 */
public abstract class MapBasedEditorViewModel<T> extends OptionEditorViewModel<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapBasedEditorViewModel.class);

    public MapBasedEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
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
                    return getItemMap().inverse().getOrDefault(object, object.toString()); // if the object is not found we simply return itself as string
                }
            }

            @Override
            public T fromString(String string) {
                if (string == null) {
                    return null;
                } else {
                    return getItemMap().getOrDefault(string, getValueFromString(string));
                }
            }
        };
    }

    /**
     * Converts a String value to the Type T. If the type cannot be directly casted to T, this method must be overriden in a subclass
     *
     * @param string The input value to convert
     * @return The value or null if the value could not be casted
     */
    @SuppressWarnings("unchecked")
    protected T getValueFromString(String string) {
        try {
            return (T) string;
        } catch (ClassCastException ex) {
            LOGGER.error(String.format("Could not cast string to type %1$s. Try overriding the method in a subclass and provide a conversion from string to the concrete type %1$s", string.getClass()), ex);
        }
        return null;
    }

    @Override
    public List<T> getItems() {
        return new ArrayList<>(getItemMap().values());
    }
}
