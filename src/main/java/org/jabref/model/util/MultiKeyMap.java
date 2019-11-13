package org.jabref.model.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MultiKeyMap<K1, K2, V> {
    private final Map<K1, Map<K2, V>> map = new HashMap<>();

    public Optional<V> get(K1 key1, K2 key2) {
        Map<K2, V> metaValue = map.get(key1);
        if (metaValue == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(metaValue.get(key2));
        }
    }

    public void put(K1 key1, K2 key2, V value) {
        Map<K2, V> metaValue = map.get(key1);
        if (metaValue == null) {
            Map<K2, V> newMetaValue = new HashMap<>();
            newMetaValue.put(key2, value);
            map.put(key1, newMetaValue);
        } else {
            metaValue.put(key2, value);
        }
    }

    public void remove(K1 key1) {
        map.remove(key1);
    }
}
