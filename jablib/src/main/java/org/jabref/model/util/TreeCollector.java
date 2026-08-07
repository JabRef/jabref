package org.jabref.model.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.TreeNode;

/// Merges a list of nodes into a tree.
/// Nodes with a common parent are added as direct children.
/// For example, the list { A > A1, A > A2, B } is transformed into the forest { A > A1, A2, B}.
public class TreeCollector {

    private TreeCollector() {
    }

    /// Merges nodes into a tree using a key to locate equivalent siblings.
    ///
    /// This avoids repeatedly scanning sibling lists when merging a large number of nodes.
    /// The key must have the same equality semantics as the groups being merged.
    // [impl->req~ux.active-library.preview-responsiveness~1]
    public static <T extends TreeNode<T>, K> ObservableList<T> mergeIntoTree(Stream<T> nodes, Function<T, K> keyExtractor) {
        ObservableList<T> roots = FXCollections.observableArrayList();
        Map<K, T> rootsByKey = new HashMap<>();
        IdentityHashMap<T, Map<K, T>> childrenByKey = new IdentityHashMap<>();

        nodes.forEach(node -> {
            K key = keyExtractor.apply(node);
            if (!rootsByKey.containsKey(key)) {
                roots.add(node);
                rootsByKey.put(key, node);
            } else {
                T matchingRoot = rootsByKey.get(key);
                for (T child : new ArrayList<>(node.getChildren())) {
                    merge(matchingRoot, child, keyExtractor, childrenByKey);
                }
            }
        });
        return roots;
    }

    private static <T extends TreeNode<T>, K> void merge(T target,
                                                         T node,
                                                         Function<T, K> keyExtractor,
                                                         IdentityHashMap<T, Map<K, T>> childrenByKey) {
        Map<K, T> targetChildrenByKey = childrenByKey.computeIfAbsent(target, currentTarget -> {
            Map<K, T> index = new HashMap<>();
            for (T child : currentTarget.getChildren()) {
                index.put(keyExtractor.apply(child), child);
            }
            return index;
        });

        K key = keyExtractor.apply(node);
        if (!targetChildrenByKey.containsKey(key)) {
            node.moveTo(target);
            targetChildrenByKey.put(key, node);
            return;
        }

        T matchingChild = targetChildrenByKey.get(key);
        for (T child : new ArrayList<>(node.getChildren())) {
            merge(matchingChild, child, keyExtractor, childrenByKey);
        }
    }
}
