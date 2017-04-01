package org.jabref.model.entry;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.ChainNode;
import org.jabref.model.util.OptionalUtil;

/**
 * Represents a keyword in a chain of keywords.
 * For example, "JabRef" in "Bibliographic manager > Awesome ones > JabRef"
 */
public class Keyword extends ChainNode<Keyword> implements Comparable<Keyword> {

    public static Character DEFAULT_HIERARCHICAL_DELIMITER = '>';
    private final String keyword;

    public Keyword(String keyword) {
        super(Keyword.class);
        this.keyword = Objects.requireNonNull(keyword).trim();
    }

    /**
     * Connects all the given keywords into one chain and returns its root,
     * e.g. "A", "B", "C" is transformed into "A > B > C".
     */
    public static Keyword of(String... keywords) {
        if (keywords.length == 0) {
            return new Keyword("");
        }

        Keyword root = new Keyword(keywords[0]);
        for (int i = 1; i < keywords.length; i++) {
            root.addAtEnd(keywords[i]);
        }
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Keyword other = (Keyword) o;
        return Objects.equals(this.keyword, other.keyword)
                // && Objects.equals(this.getParent(), other.getParent()) : we can't check the parents because then we would run in circles
                && Objects.equals(this.getChild(), other.getChild());
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword);
    }

    @Override
    public String toString() {
        return getSubchainAsString(DEFAULT_HIERARCHICAL_DELIMITER);
    }

    @Override
    public int compareTo(Keyword o) {
        return keyword.compareTo(o.keyword);
    }

    /**
     * Adds the given keyword at the end of the chain.
     * E.g., "A > B > C" + "D" -> "A > B > C > D".
     */
    private void addAtEnd(String keyword) {
        addAtEnd(new Keyword(keyword));
    }

    /**
     * Returns a text representation of the subchain starting at this item.
     * E.g., calling {@link #getSubchainAsString(Character)} on the node "B" in "A > B > C" returns "B > C".
     */
    private String getSubchainAsString(Character hierarchicalDelimiter) {
        return keyword +
                getChild().map(child -> " " + hierarchicalDelimiter + " " + child.getSubchainAsString(hierarchicalDelimiter))
                        .orElse("");
    }

    /**
     * Gets the keyword of this node in the chain.
     */
    public String get() {
        return keyword;
    }

    /**
     * Returns a text representation of the path from the root to this item.
     * E.g., calling {@link #getPathFromRootAsString(Character)} on the node "B" in "A > B > C" returns "A > B".
     */
    public String getPathFromRootAsString(Character hierarchicalDelimiter) {
        return getParent()
                .map(parent -> parent.getPathFromRootAsString(hierarchicalDelimiter) + " " + hierarchicalDelimiter + " ")
                .orElse("")
                + keyword;
    }

    /**
     * Returns all nodes in this chain as separate keywords.
     * E.g, for "A > B > C" we get {"A", "B", "C"}.
     */
    public Set<Keyword> flatten() {
        return Stream.concat(
                Stream.of(this),
                OptionalUtil.toStream(getChild()).flatMap(child -> child.flatten().stream()))
                .collect(Collectors.toSet());
    }

    /**
     * Returns all subchains starting at this node.
     * E.g., for the chain "A > B > C" the subchains {"A", "A > B", "A > B > C"} are returned.
     */
    public Set<String> getAllSubchainsAsString(Character hierarchicalDelimiter) {
        return flatten().stream()
                .map(subchain -> subchain.getPathFromRootAsString(hierarchicalDelimiter))
                .collect(Collectors.toSet());
    }
}
