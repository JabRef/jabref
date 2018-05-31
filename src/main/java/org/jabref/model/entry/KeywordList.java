package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.strings.StringUtil;

/**
 * Represents a list of keyword chains.
 * For example, "Type > A, Type > B, Something else".
 */
public class KeywordList implements Iterable<Keyword> {

    private final List<Keyword> keywordChains;

    public KeywordList() {
        keywordChains = new ArrayList<>();
    }

    public KeywordList(Collection<Keyword> keywordChains) {
        this.keywordChains = new ArrayList<>();
        keywordChains.forEach(this::add);
    }

    public KeywordList(List<String> keywordChains) {
        this(keywordChains.stream().map(Keyword::new).collect(Collectors.toList()));
    }

    public KeywordList(String... keywordChains) {
        this(Arrays.stream(keywordChains).map(Keyword::new).collect(Collectors.toList()));
    }

    public KeywordList(Keyword... keywordChains) {
        this(Arrays.asList(keywordChains));
    }

    public static KeywordList parse(String keywordString, Character delimiter, Character hierarchicalDelimiter) {
        if (StringUtil.isBlank(keywordString)) {
            return new KeywordList();
        }

        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(hierarchicalDelimiter);

        KeywordList keywordList = new KeywordList();

        StringTokenizer tok = new StringTokenizer(keywordString, delimiter.toString());
        while (tok.hasMoreTokens()) {
            String chain = tok.nextToken();
            Keyword chainRoot = Keyword.of(chain.split(hierarchicalDelimiter.toString()));
            keywordList.add(chainRoot);
        }
        return keywordList;
    }

    /**
     * Parses the keyword list and uses {@link Keyword#DEFAULT_HIERARCHICAL_DELIMITER} as hierarchical delimiter.
     *
     * @param keywordString a String of keywordChains
     * @param delimiter The delimiter used for separating the keywords
     *
     * @return an parsed list containing the keywordChains
     */
    public static KeywordList parse(String keywordString, Character delimiter) {
        return parse(keywordString, delimiter, Keyword.DEFAULT_HIERARCHICAL_DELIMITER);
    }

    public KeywordList createClone() {
        return new KeywordList(this.keywordChains);
    }

    public void replaceAll(KeywordList keywordsToReplace, Keyword newValue) {
        Objects.requireNonNull(newValue);

        // Remove keywordChains which should be replaced
        int foundPosition = -1; // remember position of the last found keyword
        for (Keyword specialFieldKeyword : keywordsToReplace) {
            int pos = keywordChains.indexOf(specialFieldKeyword);
            if (pos >= 0) {
                foundPosition = pos;
                keywordChains.remove(pos);
            }
        }

        // Add new keyword at right position
        if (foundPosition == -1) {
            add(newValue);
        } else {
            keywordChains.add(foundPosition, newValue);
        }
    }

    public void removeAll(KeywordList keywordsToRemove) {
        keywordChains.removeAll(keywordsToRemove.keywordChains);
    }

    public boolean add(Keyword keyword) {
        if (contains(keyword)) {
            return false; // Don't add duplicate keywordChains
        }
        return keywordChains.add(keyword);
    }

    /**
     * Keywords are separated by the given delimiter and an additional space, i.e. "one, two".
     */
    public String getAsString(Character delimiter) {
        return keywordChains.stream().map(Keyword::toString).collect(Collectors.joining(delimiter + " "));
    }

    public void add(String keywordsString) {
        add(new Keyword(keywordsString));
    }

    @Override
    public Iterator<Keyword> iterator() {
        return keywordChains.iterator();
    }

    public int size() {
        return keywordChains.size();
    }

    public boolean isEmpty() {
        return keywordChains.isEmpty();
    }

    public boolean contains(Keyword o) {
        return keywordChains.contains(o);
    }

    public boolean remove(Keyword o) {
        return keywordChains.remove(o);
    }

    public boolean remove(String keywordsString) {
        return keywordChains.remove(new Keyword(keywordsString));
    }

    public void addAll(KeywordList keywordsToAdd) {
        keywordChains.addAll(keywordsToAdd.keywordChains);
    }

    public void retainAll(KeywordList keywordToRetain) {
        keywordChains.retainAll(keywordToRetain.keywordChains);
    }

    public void clear() {
        keywordChains.clear();
    }

    public Keyword get(int index) {
        return keywordChains.get(index);
    }

    public Stream<Keyword> stream() {
        return keywordChains.stream();
    }

    @Override
    public String toString() {
        return getAsString(',');
    }

    public Set<String> toStringList() {
        return keywordChains.stream().map(Keyword::toString).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        KeywordList keywords1 = (KeywordList) o;
        return Objects.equals(keywordChains, keywords1.keywordChains);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keywordChains);
    }
}
