package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sf.jabref.model.strings.StringUtil;

/**
 * Represents a list of keyword chains.
 * For example, "Type > A, Type > B, Something else".
 */
public class KeywordList implements Iterable<Keyword> {

    private final List<Keyword> keywords;

    public KeywordList() {
        keywords = new ArrayList<>();
    }

    public KeywordList(Collection<Keyword> keywords) {
        this.keywords = new ArrayList<>();
        keywords.forEach(this::add);
    }

    public KeywordList(List<String> keywords) {
        this(keywords.stream().map(Keyword::new).collect(Collectors.toList()));
    }

    public KeywordList(String... keywords) {
        this(Arrays.stream(keywords).map(Keyword::new).collect(Collectors.toList()));
    }

    /**
     * @param keywordString a String of keywords
     * @return an parsed list containing the keywords
     */
    public static KeywordList parse(String keywordString, Character delimiter) {
        if (StringUtil.isBlank(keywordString)) {
            return new KeywordList();
        }

        List<String> keywords = new ArrayList<>();

        StringTokenizer tok = new StringTokenizer(keywordString, delimiter.toString());
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken().trim();
            keywords.add(word);
        }
        return new KeywordList(keywords);
    }

    public KeywordList createClone() {
        return new KeywordList(this.keywords);
    }

    public void replaceAll(KeywordList keywordsToReplace, Keyword newValue) {
        Objects.requireNonNull(newValue);

        // Remove keywords which should be replaced
        int foundPosition = -1; // remember position of the last found keyword
        for (Keyword specialFieldKeyword : keywordsToReplace) {
            int pos = keywords.indexOf(specialFieldKeyword);
            if (pos >= 0) {
                foundPosition = pos;
                keywords.remove(pos);
            }
        }

        // Add new keyword at right position
        if (foundPosition == -1) {
            add(newValue);
        } else {
            keywords.add(foundPosition, newValue);
        }
    }

    public void removeAll(KeywordList keywordsToRemove) {
        keywords.removeAll(keywordsToRemove.keywords);
    }

    /**
     * @deprecated use {@link #replaceAll(KeywordList, Keyword)} or {@link #removeAll(KeywordList)}
     */
    @Deprecated
    public void replaceKeywords(KeywordList keywordsToReplace, Optional<Keyword> newValue) {
        if (newValue.isPresent()) {
            replaceAll(keywordsToReplace, newValue.get());
        } else {
            removeAll(keywordsToReplace);
        }
    }

    public boolean add(Keyword keyword) {
        if (contains(keyword)) {
            return false; // Don't add duplicate keywords
        }
        return keywords.add(keyword);
    }

    /**
     * Keywords are separated by the given delimiter and an additional space, i.e. "one, two".
     */
    public String getAsString(Character delimiter) {
        return keywords.stream().map(Keyword::toString).collect(Collectors.joining(delimiter + " "));
    }

    public void add(String keywordsString) {
        add(new Keyword(keywordsString));
    }

    @Override
    public Iterator<Keyword> iterator() {
        return keywords.iterator();
    }

    public int size() {
        return keywords.size();
    }

    public boolean isEmpty() {
        return keywords.isEmpty();
    }

    public boolean contains(Keyword o) {
        return keywords.contains(o);
    }

    public boolean remove(Keyword o) {
        return keywords.remove(o);
    }

    public boolean remove(String keywordsString) {
        return keywords.remove(new Keyword(keywordsString));
    }

    public void addAll(KeywordList keywordsToAdd) {
        keywords.addAll(keywordsToAdd.keywords);
    }

    public void retainAll(KeywordList keywordToRetain) {
        keywords.retainAll(keywordToRetain.keywords);
    }

    public void clear() {
        keywords.clear();
    }

    public Keyword get(int index) {
        return keywords.get(index);
    }

    public Stream<Keyword> stream() {
        return keywords.stream();
    }

    @Override
    public String toString() {
        return getAsString(',');
    }

    public Set<String> toStringList() {
        return keywords.stream().map(Keyword::toString).collect(Collectors.toSet());
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
        return Objects.equals(keywords, keywords1.keywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keywords);
    }
}
