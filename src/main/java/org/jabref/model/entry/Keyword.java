package org.jabref.model.entry;

import java.util.Objects;

import org.jabref.model.ChainNode;

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
     * e.g. "A", "B", "C" is transformed to "A > B > C".
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
        return getAsString(DEFAULT_HIERARCHICAL_DELIMITER);
    }

    @Override
    public int compareTo(Keyword o) {
        return keyword.compareTo(o.keyword);
    }

    private void addAtEnd(String keyword) {
        addAtEnd(new Keyword(keyword));
    }

    public String getAsString(Character hierarchicalDelimiter) {
        return keyword +
                getChild().map(child -> " " + hierarchicalDelimiter + " " + child.getAsString(hierarchicalDelimiter)).orElse("");
    }
}
