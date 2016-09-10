package net.sf.jabref.model.entry;

import java.util.Objects;

import net.sf.jabref.model.ChainNode;

/**
 * Represents a keyword in a chain of keywords.
 * For example, "JabRef" in "Bibliographic manager > Awesome ones > JabRef"
 */
public class Keyword extends ChainNode<Keyword> implements Comparable<Keyword> {

    private final String keyword;

    public Keyword(String keyword) {
        super(Keyword.class);
        this.keyword = Objects.requireNonNull(keyword);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(keyword, ((Keyword) o).keyword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword);
    }

    @Override
    public String toString() {
        return keyword;
    }

    @Override
    public int compareTo(Keyword o) {
        return keyword.compareTo(o.keyword);
    }
}
