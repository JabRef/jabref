package org.jabref.logic.journals.ltwa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrefixTree<D> {
    /**
     * Wildcard character used to match any character in the prefix tree.
     * For example, WILD_CARD + WILD_CARD + WILD_CARD + "Hello World" -> would match .{,3}Hello World in this prefix tree
     */
    public static final int WILD_CARD = '*';
    private final Node<D> root;

    public PrefixTree() {
        this.root = new Node<>();
    }

    public void insert(String key, List<D> data) {
        StringBuilder normalized = new StringBuilder();
        boolean lastWasWildcard = false;

        for (int i = 0; i < key.length(); ) {
            int codepoint = key.codePointAt(i);
            int charCount = Character.charCount(codepoint);

            if (codepoint == WILD_CARD) {
                if (!lastWasWildcard) {
                    normalized.appendCodePoint(codepoint);
                    lastWasWildcard = true;
                }
            } else {
                normalized.appendCodePoint(codepoint);
                lastWasWildcard = false;
            }

            i += charCount;
        }

        root.insert(normalized.toString(), data);
    }

    public List<D> search(String word) {
        Set<D> result = new HashSet<>();
        searchRecursive(root, word, 0, result, new HashSet<>());
        return new ArrayList<>(result);
    }

    private void searchRecursive(Node<D> node, String word, int index, Set<D> result, Set<SearchState> visited) {
        result.addAll(node.data);

        if (index == word.length()) {
            return;
        }

        SearchState state = new SearchState(node, index);
        if (visited.contains(state)) {
            return;
        }
        visited.add(state);

        int codepoint = word.codePointAt(index);
        int charCount = Character.charCount(codepoint);

        Node<D> exactMatch = node.children.get(codepoint);
        if (exactMatch != null) {
            searchRecursive(exactMatch, word, index + charCount, result, visited);
        }

        Node<D> wildcardMatch = node.children.get(WILD_CARD);
        if (wildcardMatch != null) {
            searchRecursive(wildcardMatch, word, index, result, visited);
            if (index + charCount <= word.length()) {
                searchRecursive(wildcardMatch, word, index + charCount, result, visited);
            }
        }
    }

    private record SearchState(Node<?> node, int index) {
    }

    private static class Node<D> {
        private final Map<Integer, Node<D>> children;
        private final List<D> data;

        public Node() {
            this.children = new HashMap<>();
            this.data = new ArrayList<>();
        }

        public void insert(String key, List<D> data) {
            Node<D> current = this;
            for (int i = 0; i < key.length(); ) {
                int codepoint = key.codePointAt(i);
                i += Character.charCount(codepoint);
                current = current.children.computeIfAbsent(codepoint, _ -> new Node<>());
            }
            current.data.addAll(data);
        }
    }
}
