package org.jabref.model.groups;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.LatexToUnicodeAdapter;

public class AutomaticPersonsGroup extends AutomaticGroup {

    private static final Map<String, List<String>> CACHED_LASTNAMES = new HashMap<>();
    private final Field field;

    public AutomaticPersonsGroup(String name, GroupHierarchyType context, Field field) {
        super(name, context);
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AutomaticPersonsGroup that = (AutomaticPersonsGroup) o;
        return Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticPersonsGroup(this.name.getValue(), this.context, this.field);
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        return getAsLastNamesLatexFree(field, entry).stream()
                                                    .map(lastName -> new WordKeywordGroup(lastName, GroupHierarchyType.INDEPENDENT,
                                                            field, lastName, true, ' ', new LastNameSearchStrategy(lastName, field)))
                                                    .map(GroupTreeNode::new)
                                                    .collect(Collectors.toSet());
    }

    private static List<String> getAsLastNamesLatexFree(Field field, BibEntry bibEntry) {
        final String unparsedAuthorList = bibEntry.getField(field).orElse(null);
        List<String> lastNames = CACHED_LASTNAMES.get(unparsedAuthorList);
        if (lastNames != null) {
            return lastNames;
        }

        lastNames = bibEntry.getField(field)
                            .map(AuthorList::parse)
                            .map(AuthorList::getAuthors)
                            .map(authors ->
                                    authors.stream()
                                           .map(Author::getLast)
                                           .flatMap(Optional::stream)
                                           .map(LatexToUnicodeAdapter::format)
                                           .collect(Collectors.toList()))
                            .orElse(Collections.emptyList());

        CACHED_LASTNAMES.put(unparsedAuthorList, lastNames);
        return lastNames;
    }

    private static class LastNameSearchStrategy implements WordKeywordGroup.SearchStrategy {
        private final Field field;
        private final String lastName;

        public LastNameSearchStrategy(String lastName, Field field) {
            this.field = field;
            this.lastName = lastName;
        }

        @Override
        public boolean contains(BibEntry entry) {
            return getAsLastNamesLatexFree(field, entry).stream().anyMatch(name -> name.equals(lastName));
        }
    }

    public Field getField() {
        return field;
    }
}
