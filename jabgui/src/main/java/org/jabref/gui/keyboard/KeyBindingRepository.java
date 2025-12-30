package org.jabref.gui.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * Handles keyboard shortcuts. Including checking whether a keybinding matches.
 * See {@link #matches}.
 */
public class KeyBindingRepository {

    /**
     * sorted by localization
     */
    private final MapProperty<KeyBinding, String> bindings;

    public KeyBindingRepository() {
        this(List.of(), List.of());
    }

    public KeyBindingRepository(SortedMap<KeyBinding, String> bindings) {
        this.bindings = new SimpleMapProperty<>(FXCollections.observableMap(bindings));
    }

    public KeyBindingRepository(List<String> bindNames, List<String> bindings) {
        this.bindings = new SimpleMapProperty<>(FXCollections.observableMap(new TreeMap<>(Comparator.comparing(KeyBinding::getLocalization))));

        if ((bindNames.isEmpty()) || (bindings.isEmpty()) || (bindNames.size() != bindings.size())) {
            // Use default key bindings
            for (KeyBinding keyBinding : KeyBinding.values()) {
                put(keyBinding, keyBinding.getDefaultKeyBinding());
            }
        } else {
            for (int i = 0; i < bindNames.size(); i++) {
                put(bindNames.get(i), bindings.get(i));
            }
        }
    }

    /**
     * Check if the given keyCombination equals the given keyEvent
     *
     * @param combination as KeyCombination
     * @param keyEvent    as KeEvent
     * @return true if matching, else false
     */
    public static boolean checkKeyCombinationEquality(KeyCombination combination, KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        if (code == KeyCode.UNDEFINED) {
            return false;
        }

        return combination.match(keyEvent);
    }

    public Optional<String> get(KeyBinding key) {
        return Optional.ofNullable(bindings.get(key));
    }

    public String get(String key) {
        Optional<KeyBinding> keyBinding = getKeyBinding(key);
        Optional<String> result = keyBinding.flatMap(k -> Optional.ofNullable(bindings.get(k)));

        if (result.isPresent()) {
            return result.get();
        } else if (keyBinding.isPresent()) {
            return keyBinding.get().getDefaultKeyBinding();
        } else {
            return "Not associated";
        }
    }

    /**
     * Returns the HashMap containing all key bindings.
     */
    public SortedMap<KeyBinding, String> getKeyBindings() {
        return new TreeMap<>(bindings);
    }

    public void put(KeyBinding key, String value) {
        bindings.put(key, value);
    }

    public void put(String key, String value) {
        getKeyBinding(key).ifPresent(binding -> put(binding, value));
    }

    private Optional<KeyBinding> getKeyBinding(String key) {
        return Arrays.stream(KeyBinding.values()).filter(b -> b.getConstant().equals(key)).findFirst();
    }

    public void resetToDefault(String key) {
        getKeyBinding(key).ifPresent(b -> bindings.put(b, b.getDefaultKeyBinding()));
    }

    public void resetToDefault() {
        bindings.forEach((b, s) -> bindings.put(b, b.getDefaultKeyBinding()));
    }

    public int size() {
        return this.bindings.size();
    }

    /**
     * Searches the key bindings for the given KeyEvent. Only the first matching key binding is returned.
     * <p>
     * If you need all matching key bindings, use {@link #mapToKeyBindings(KeyEvent)} instead.
     */
    public Optional<KeyBinding> mapToKeyBinding(KeyEvent keyEvent) {
        for (KeyBinding binding : KeyBinding.values()) {
            if (checkKeyCombinationEquality(binding, keyEvent)) {
                return Optional.of(binding);
            }
        }
        return Optional.empty();
    }

    /**
     * Used if the same key could be used by multiple actions
     */
    private Set<KeyBinding> mapToKeyBindings(KeyEvent keyEvent) {
        return Arrays.stream(KeyBinding.values())
                     .filter(binding -> checkKeyCombinationEquality(binding, keyEvent))
                     .collect(Collectors.toSet());
    }

    /**
     * Checks if the given KeyEvent matches the given KeyBinding.
     * <p>
     * Used if a keyboard shortcut leads to multiple actions (e.g., ESC for closing a dialog and clearing the search).
     */
    public boolean matches(KeyEvent event, KeyBinding keyBinding) {
        return mapToKeyBindings(event)
                .stream()
                .anyMatch(binding -> binding == keyBinding);
    }

    public Optional<KeyCombination> getKeyCombination(KeyBinding bindName) {
        String binding = get(bindName.getConstant());
        if (binding.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(KeyCombination.valueOf(binding));
    }

    /**
     * Check if the given KeyBinding equals the given keyEvent
     *
     * @param binding  as KeyBinding
     * @param keyEvent as KeEvent
     * @return true if matching, else false
     */
    public boolean checkKeyCombinationEquality(KeyBinding binding, KeyEvent keyEvent) {
        return getKeyCombination(binding).filter(combination -> checkKeyCombinationEquality(combination, keyEvent))
                                         .isPresent();
    }

    public List<String> getBindNames() {
        return bindings.keySet().stream().map(KeyBinding::getConstant).collect(Collectors.toList());
    }

    public List<String> getBindings() {
        return new ArrayList<>(bindings.values());
    }

    public MapProperty<KeyBinding, String> getBindingsProperty() {
        return bindings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        KeyBindingRepository that = (KeyBindingRepository) o;

        return bindings.equals(that.bindings);
    }

    @Override
    public int hashCode() {
        return bindings.hashCode();
    }
}
