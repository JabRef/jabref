/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.keyboard;

import java.util.Arrays;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public class KeyBindingRepository {

    /**
     * sorted by localization
     */
    private final SortedMap<KeyBinding, String> bindings;

    public KeyBindingRepository() {
        bindings = new TreeMap<>((k1, k2) -> k1.getLocalization().compareTo(k2.getLocalization()));
        for (KeyBinding keyBinding : KeyBinding.values()) {
            bindings.put(keyBinding, keyBinding.getDefaultBinding());
        }
    }

    public KeyBindingRepository(SortedMap<KeyBinding, String> bindings) {
        this.bindings = bindings;
    }

    public Optional<String> get(KeyBinding key) {
        return getKeyBinding(key).flatMap(k -> Optional.ofNullable(bindings.get(k)));
    }

    public String get(String key) {
        Optional<KeyBinding> keyBinding = getKeyBinding(key);
        Optional<String> result  = keyBinding.flatMap(k -> Optional.ofNullable(bindings.get(k)));

        if(result.isPresent()) {
            return result.get();
        } else if(keyBinding.isPresent()){
            return keyBinding.get().getDefaultBinding();
        } else {
            return "Not associated";
        }
    }

    public SortedMap<KeyBinding, String> getKeyBindings() {
        return new TreeMap<>(bindings);
    }

    public void overwriteBindings(SortedMap<KeyBinding, String> newBindings) {
        bindings.clear();
        newBindings.forEach(this::put);
    }

    public void put(KeyBinding key, String value) {
        getKeyBinding(key).ifPresent(binding -> bindings.put(binding, value));
    }

    public void put(String key, String value) {
        getKeyBinding(key).ifPresent(binding -> bindings.put(binding, value));
    }

    private Optional<KeyBinding> getKeyBinding(String key) {
        return Arrays.stream(KeyBinding.values()).filter(b -> b.getKey().equals(key)).findFirst();
    }

    private Optional<KeyBinding> getKeyBinding(KeyBinding key) {
        return Arrays.stream(KeyBinding.values()).filter(b -> b.equals(key)).findFirst();
    }

    public void resetToDefault(String key) {
        getKeyBinding(key).ifPresent(b -> bindings.put(b, b.getDefaultBinding()));
    }

    public void resetToDefault() {
        bindings.forEach((b, s) -> bindings.put(b, b.getDefaultBinding()));
    }

    public int size() {
        return this.bindings.size();
    }

}
