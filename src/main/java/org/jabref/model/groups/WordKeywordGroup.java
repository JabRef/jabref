package org.jabref.model.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.strings.StringUtil;

/**
 * Matches entries if a given field contains a specified word.
 */
public class WordKeywordGroup extends KeywordGroup implements GroupEntryChanger {

    protected final Character keywordSeparator;
    private final Set<String> searchWords;
    private final boolean onlySplitWordsAtSeparator;

    public WordKeywordGroup(String name, GroupHierarchyType context, String searchField,
            String searchExpression, boolean caseSensitive, Character keywordSeparator,
            boolean onlySplitWordsAtSeparator) {
        super(name, context, searchField, searchExpression, caseSensitive);

        this.keywordSeparator = keywordSeparator;
        this.onlySplitWordsAtSeparator = onlySplitWordsAtSeparator;
        this.searchWords = getSearchWords(searchExpression);
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
    public List<FieldChange> add(List<BibEntry> entriesToAdd) {
        Objects.requireNonNull(entriesToAdd);

        List<FieldChange> changes = new ArrayList<>();
        for (BibEntry entry : entriesToAdd) {
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
        for (BibEntry entry : entriesToRemove) {
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
        Set<String> content = getFieldContentAsWords(entry);
        if (caseSensitive) {
            return content.containsAll(searchWords);
        } else {
            return containsCaseInsensitive(content, searchWords);
        }
    }

    private Set<String> getFieldContentAsWords(BibEntry entry) {
        if (onlySplitWordsAtSeparator) {
            return entry.getField(searchField)
                    .map(content -> KeywordList.parse(content, keywordSeparator).toStringList())
                    .orElse(Collections.emptySet());
        } else {
            return entry.getFieldAsWords(searchField);
        }
    }

    private Set<String> getSearchWords(String searchExpression) {
        if (onlySplitWordsAtSeparator) {
            return KeywordList.parse(searchExpression, keywordSeparator).toStringList();
        } else {
            return new HashSet<>(StringUtil.getStringAsWords(searchExpression));
        }
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
}
