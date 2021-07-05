package org.jabref.model.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.ListUtil;

/**
 * Matches entries if a given field contains a specified word.
 */
public class WordKeywordGroup extends KeywordGroup implements GroupEntryChanger {

    protected final Character keywordSeparator;
    private final SearchStrategy searchStrategy;
    private final boolean onlySplitWordsAtSeparator;

    public WordKeywordGroup(String name, GroupHierarchyType context, Field searchField,
                            String searchExpression, boolean caseSensitive, Character keywordSeparator,
                            boolean onlySplitWordsAtSeparator) {
        super(name, context, searchField, searchExpression, caseSensitive);

        this.keywordSeparator = keywordSeparator;
        this.onlySplitWordsAtSeparator = onlySplitWordsAtSeparator;

        if (onlySplitWordsAtSeparator) {
            if (InternalField.TYPE_HEADER.equals(searchField)) {
                searchStrategy = new TypeSearchStrategy();
            } else {
                searchStrategy = new KeywordListSearchStrategy();
            }
        } else {
            searchStrategy = new StringSearchStrategy();
        }
    }

    private static boolean containsCaseInsensitive(Set<String> searchIn, Collection<String> searchFor) {
        for (String searchWord : searchFor) {
            if (!containsCaseInsensitive(searchIn, searchWord)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsCaseInsensitive(Set<String> searchIn, String searchFor) {
        for (String word : searchIn) {
            if (word.equalsIgnoreCase(searchFor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<FieldChange> add(Collection<BibEntry> entriesToAdd) {
        Objects.requireNonNull(entriesToAdd);

        List<FieldChange> changes = new ArrayList<>();
        for (BibEntry entry : new ArrayList<>(entriesToAdd)) {
            if (!contains(entry)) {
                String oldContent = entry.getField(searchField).orElse("");
                KeywordList wordlist = KeywordList.parse(oldContent, keywordSeparator);
                wordlist.add(searchExpression);
                String newContent = wordlist.getAsString(keywordSeparator);
                entry.setField(searchField, newContent).ifPresent(changes::add);
            }
        }
        return changes;
    }

    @Override
    public List<FieldChange> remove(List<BibEntry> entriesToRemove) {
        Objects.requireNonNull(entriesToRemove);
        List<FieldChange> changes = new ArrayList<>();
        for (BibEntry entry : new ArrayList<>(entriesToRemove)) {
            if (contains(entry)) {
                String oldContent = entry.getField(searchField).orElse("");
                KeywordList wordlist = KeywordList.parse(oldContent, keywordSeparator);
                wordlist.remove(searchExpression);
                String newContent = wordlist.getAsString(keywordSeparator);
                entry.setField(searchField, newContent).ifPresent(changes::add);
            }
        }
        return changes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WordKeywordGroup)) {
            return false;
        }
        WordKeywordGroup other = (WordKeywordGroup) o;
        return Objects.equals(getName(), other.getName())
                && Objects.equals(getHierarchicalContext(), other.getHierarchicalContext())
                && Objects.equals(searchField, other.searchField)
                && Objects.equals(searchExpression, other.searchExpression)
                && Objects.equals(caseSensitive, other.caseSensitive)
                && Objects.equals(keywordSeparator, other.keywordSeparator)
                && Objects.equals(onlySplitWordsAtSeparator, other.onlySplitWordsAtSeparator);
    }

    @Override
    public boolean contains(BibEntry entry) {
        return searchStrategy.contains(entry);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new WordKeywordGroup(getName(), getHierarchicalContext(), searchField, searchExpression,
                caseSensitive, keywordSeparator, onlySplitWordsAtSeparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(),
                getHierarchicalContext(),
                searchField,
                searchExpression,
                caseSensitive,
                keywordSeparator,
                onlySplitWordsAtSeparator);
    }

    interface SearchStrategy {
        boolean contains(BibEntry entry);
    }

    class StringSearchStrategy implements SearchStrategy {
        Set<String> searchWords;

        StringSearchStrategy() {
            searchWords = new HashSet<>(StringUtil.getStringAsWords(searchExpression));
        }

        @Override
        public boolean contains(BibEntry entry) {
            Set<String> content = entry.getFieldAsWords(searchField);
            if (caseSensitive) {
                return content.containsAll(searchWords);
            } else {
                return containsCaseInsensitive(content, searchWords);
            }
        }
    }

    class TypeSearchStrategy implements SearchStrategy {

        Set<EntryType> searchWords;

        TypeSearchStrategy() {
            searchWords = KeywordList.parse(searchExpression, keywordSeparator)
                                     .stream()
                                     .map(word -> EntryTypeFactory.parse(word.get()))
                                     .collect(Collectors.toSet());
        }

        @Override
        public boolean contains(BibEntry entry) {
            return searchWords.stream()
                              .anyMatch(word -> entry.getType().equals(word));
        }
    }

    class KeywordListSearchStrategy implements SearchStrategy {

        private final KeywordList searchWords;

        KeywordListSearchStrategy() {
            searchWords = KeywordList.parse(searchExpression, keywordSeparator);
        }

        @Override
        public boolean contains(BibEntry entry) {
            KeywordList fieldValue = entry.getFieldAsKeywords(searchField, keywordSeparator);
            return ListUtil.allMatch(searchWords, fieldValue::contains);
        }
    }
}
