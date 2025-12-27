package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NonNull;

/**
 * Represents a list of keyword chains.
 * For example, "Type > A, Type > B, Something else".
 */
@AllowedToUseLogic("Uses StringUtil temporarily")
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

    /**
     * Parses the keyword list and uses {@link Keyword#DEFAULT_HIERARCHICAL_DELIMITER} as hierarchical delimiter.
     *
     * @param keywordString a String of keywordChains
     * @param delimiter     The delimiter used for separating the keywords
     * @return a parsed list containing the keywordChains
     */
    public static KeywordList parse(@NonNull String keywordString, @NonNull Character delimiter) {
        if (StringUtil.isBlank(keywordString)) {
            return new KeywordList();
        }

        KeywordList keywordList = new KeywordList();
        List<String> hierarchy = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        AtomicBoolean isEscaping = new AtomicBoolean(false);

        keywordString.chars().forEachOrdered(symbol -> {
            char currentChar = (char) symbol;
            if (isEscaping.get()) {
                currentToken.append(currentChar);
                isEscaping.set(false);
            } else if (currentChar == '\\') {
                isEscaping.set(true);
            } else if (currentChar == Keyword.DEFAULT_HIERARCHICAL_DELIMITER) {
                hierarchy.add(currentToken.toString().trim());
                currentToken.setLength(0);
            } else if (currentChar == delimiter) {
                hierarchy.add(currentToken.toString().trim());
                keywordList.add(Keyword.of(hierarchy));
                hierarchy.clear();
                currentToken.setLength(0);
            } else {
                currentToken.append(currentChar);
            }
        });

        if (!currentToken.isEmpty() || !hierarchy.isEmpty()) {
            hierarchy.add(currentToken.toString().trim());
            keywordList.add(Keyword.of(hierarchy));
        }

        return keywordList;
    }

    public static String serialize(List<Keyword> keywords, Character delimiter) {
        String delimiterStr = delimiter.toString();
        String escapedDelimiter = "\\" + delimiterStr;
        String hierarchicalDelimiterStr = Keyword.DEFAULT_HIERARCHICAL_DELIMITER.toString();
        String escapedHierarchicalDelimiter = "\\" + hierarchicalDelimiterStr;
        String hierarchicalSeparator = " " + hierarchicalDelimiterStr + " ";

        return keywords.stream()
                       .map(keyword -> keyword.flatten().stream()
                                              .map(Keyword::get)
                                              .map(nodeKeyword -> nodeKeyword.replace("\\", "\\\\"))
                                              .map(nodeKeyword -> nodeKeyword.replace(delimiterStr, escapedDelimiter))
                                              .map(nodeKeyword -> nodeKeyword.replace(hierarchicalDelimiterStr, escapedHierarchicalDelimiter))
                                              .collect(Collectors.joining(hierarchicalSeparator)))
                       .collect(Collectors.joining(delimiterStr));
    }

    public static KeywordList merge(String keywordStringA, String keywordStringB, Character delimiter) {
        KeywordList keywordListA = parse(keywordStringA, delimiter);
        KeywordList keywordListB = parse(keywordStringB, delimiter);
        List<Keyword> distinctKeywords = Stream.concat(keywordListA.stream(), keywordListB.stream()).distinct().toList();
        return new KeywordList(distinctKeywords);
    }

    public KeywordList createClone() {
        return new KeywordList(this.keywordChains);
    }

    public void replaceAll(KeywordList keywordsToReplace, @NonNull Keyword newValue) {
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

    public boolean contains(String keywordString) {
        return contains(new Keyword(keywordString));
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
        return Objects.equals(new HashSet<>(keywordChains), new HashSet<>(keywords1.keywordChains));
    }

    @Override
    public int hashCode() {
        return Objects.hash(keywordChains);
    }
}
