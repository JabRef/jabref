package org.jabref.gui.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import org.jabref.logic.util.OS;

public class KeyBindingRepository {

    /**
     * sorted by localization
     */
    private final SortedMap<KeyBinding, String> bindings;

    public KeyBindingRepository() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public KeyBindingRepository(SortedMap<KeyBinding, String> bindings) {
        this.bindings = bindings;
    }

    public KeyBindingRepository(List<String> bindNames, List<String> bindings) {
        this.bindings = new TreeMap<>(Comparator.comparing(KeyBinding::getLocalization));

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

    public Optional<KeyBinding> mapToKeyBinding(KeyEvent keyEvent) {
        for (KeyBinding binding : KeyBinding.values()) {
            if (checkKeyCombinationEquality(binding, keyEvent)) {
                return Optional.of(binding);
            }
        }
        return Optional.empty();
    }

    public Optional<KeyCombination> getKeyCombination(KeyBinding bindName) {
        String binding = get(bindName.getConstant());
        if (binding.isEmpty()) {
            return Optional.empty();
        }
        if (OS.OS_X) {
            binding = binding.replace("ctrl", "meta");
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
